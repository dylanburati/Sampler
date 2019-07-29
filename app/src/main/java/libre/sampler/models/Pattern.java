package libre.sampler.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;
import libre.sampler.utils.IdStatus;
import libre.sampler.utils.LoopScheduler;
import libre.sampler.utils.MusicTime;

@Entity(tableName = "pattern", primaryKeys = {"projectId", "id"})
public class Pattern {
    public static final double DEFAULT_TEMPO = 140;
    public static final MusicTime DEFAULT_LOOP_LENGTH = new MusicTime(4, 0, 0);

    public int projectId;
    public int id;
    public String name;

    @Ignore
    private int nextEventId;
    @Ignore
    private List<ScheduledNoteEvent> events;
    @Ignore
    private IdStatus idStatus = new IdStatus("Pattern,ScheduledNoteEvent");
    @Ignore
    private ScheduledNoteEvent[] eventsDeepCopy;

    private double nanosPerTick;
    private long loopLengthTicks;

    @Ignore
    private long checkpointTicks;
    @Ignore
    private long checkpointNanos;

    @Ignore
    public LoopScheduler scheduler;

    @Ignore
    private int playingState;

    private static final int STOPPED = 0;
    private static final int PLAYING = 1;
    private static final int PAUSED = 2;
    public boolean isStarted() { return playingState != 0; }
    public boolean isPlaying() { return playingState == 1; }
    public boolean isPaused() { return playingState == 2; }

    @Ignore
    private Pattern(String name) {
        this.name = name;
        this.events = new ArrayList<>();
        // Placeholder event so scheduler loopIndex is always incremented
        this.events.add(ScheduledNoteEvent.getPlaceholder());
        this.scheduler = new LoopScheduler(this.events, this);
        this.nextEventId = 1;
    }

    // should be called with the `id` obtained from the database
    public Pattern(int id, String name) {
        this.name = name;
        this.id = id;

        idStatus.set(IdStatus.SELF);
    }

    public void setPatternId(int id) {
        this.id = id;
        for(ScheduledNoteEvent e : events) {
            e.patternId = id;
        }

        idStatus.set(IdStatus.SELF);
    }

    // should be called with the `events` obtained from the database
    public void setEvents(List<ScheduledNoteEvent> events) {
        this.events = events;
        for(ScheduledNoteEvent e : events) {
            if(e.id >= nextEventId) {
                nextEventId = e.id + 1;
            }
        }
        this.scheduler = new LoopScheduler(this.events, this);

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_DB);
    }

    public List<ScheduledNoteEvent> getEvents() {
        return events;
    }

    public void prepareEventsDeepCopy() {
        synchronized(scheduler) {
            eventsDeepCopy = events.toArray(new ScheduledNoteEvent[0]);
        }
    }

    public ScheduledNoteEvent[] getEventsDeepCopy() {
        return eventsDeepCopy;
    }

    public long getLoopLengthTicks() {
        return loopLengthTicks;
    }

    // All of the following methods should be called
    // by a thread holding the patternThread.lock
    public void setLoopLengthTicks(long ticks) {
        if(isStarted()) {
            scheduler.cancelPending();
            long now = System.nanoTime();
            long phantomTicks = scheduler.getLoopIndex() * (ticks - this.loopLengthTicks);
            checkpointTicks = getTicksAtTime(now) + phantomTicks;
            checkpointNanos = now;
        }
        this.loopLengthTicks = ticks;
    }

    public void setTempo(double bpm) {
        double updatedNanosPerTick = (60 * 1e9 / (1.0 * MusicTime.TICKS_PER_BEAT * bpm));
        setNanosPerTick(updatedNanosPerTick);
    }

    public double getTempo() {
        if(nanosPerTick == 0) {
            return 0;
        }
        return (60 * 1e9 / (1.0 * MusicTime.TICKS_PER_BEAT * nanosPerTick));
    }

    public double getNanosPerTick() {
        return nanosPerTick;
    }

    public void setNanosPerTick(double updatedNanosPerTick) {
        if(isPlaying()) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now);
            checkpointNanos = now;
            scheduler.cancelPending();
        }
        nanosPerTick = updatedNanosPerTick;
    }

    public void addEvent(ScheduledNoteEvent noteEvent) {
        noteEvent.patternId = this.id;
        noteEvent.id = nextEventId;
        nextEventId++;

        synchronized(scheduler) {
            int insertIdx = Collections.binarySearch(events, noteEvent, new Comparator<ScheduledNoteEvent>() {
                @Override
                public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                    return Long.compare(o1.offsetTicks, o2.offsetTicks);
                }
            });
            if(insertIdx < 0) {
                insertIdx = -insertIdx - 1;
            }

            scheduler.doInsert(insertIdx, noteEvent, getTicksAtTime(System.nanoTime()));
        }

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public boolean removeEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        synchronized(scheduler) {
            int removeIdx = events.indexOf(event);
            if(removeIdx != -1) {
                scheduler.doRemove(removeIdx, event);
                return true;
            }
            return false;
        }
    }

    public NoteEvent removeAndGetEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        synchronized(scheduler) {
            int removeIdx = events.indexOf(event);
            if(removeIdx != -1) {
                return scheduler.doRemove(removeIdx, event);
            }
            return null;
        }
    }

    public void start() {
        synchronized(scheduler) {
            playingState = PLAYING;
            checkpointNanos = System.nanoTime();
            checkpointTicks = 0;
            scheduler.reset();
        }
    }

    public void pause() {
        synchronized(scheduler) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now);
            checkpointNanos = now;
            scheduler.cancelPending();
            playingState = PAUSED;
        }
    }

    public void resume() {
        if(!isPaused()) {
            return;
        }
        synchronized(scheduler) {
            playingState = PLAYING;
            checkpointNanos = System.nanoTime();
        }
    }

    public void stop() {
        playingState = STOPPED;
    }

    public long getTicksAtTime(long time) {
        if(!isStarted()) {
            return 0;
        }
        if(isPaused()) {
            return checkpointTicks;
        }
        long afterCheckpointTicks = (long) ((time - checkpointNanos) / nanosPerTick);
        return checkpointTicks + afterCheckpointTicks;
    }

    private long getTimeAtTicks(long ticks) {
        return (long) ((ticks - checkpointTicks) * this.nanosPerTick + checkpointNanos);
    }

    public long getTimeToNextEvent() {
        if(events.size() == 0) {
            return Long.MAX_VALUE;
        }
        return getTimeAtTicks(scheduler.getNextEventTicks()) - System.nanoTime();
    }

    public void getNextEvents(NoteEvent[] noteEventsOut, long tolerance) {
        synchronized(scheduler) {
            scheduler.cancelPending();
            long firstEventTime = Long.MAX_VALUE;

            for(int i = 0; i < noteEventsOut.length; i++) {
                long eventTime = getTimeAtTicks(scheduler.getNextEventTicks());

                if(eventTime - firstEventTime < tolerance) {
                    if(i == 0) {
                        firstEventTime = eventTime;
                    }
                    noteEventsOut[i] = scheduler.getNextNoteEvent();
                } else {
                    return;
                }
            }
        }
    }

    public static Pattern getEmptyPattern() {
        Pattern retval = new Pattern("");
        retval.setLoopLengthTicks(DEFAULT_LOOP_LENGTH.getTicks());
        retval.setTempo(DEFAULT_TEMPO);
        return retval;
    }
}
