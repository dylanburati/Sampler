package libre.sampler.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;
import libre.sampler.utils.IdStatus;
import libre.sampler.utils.MusicTime;

@Entity(tableName = "pattern", primaryKeys = {"projectId", "id"})
public class Pattern {
    public static final double DEFAULT_TEMPO = 140;
    public static final MusicTime DEFAULT_LOOP_LENGTH = new MusicTime(1, 0, 0);

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
    private int schedulerEventIndex;
    @Ignore
    private int schedulerLoopIndex;

    @Ignore
    public boolean isStarted;
    @Ignore
    public boolean isPaused;

    @Ignore
    private Pattern(String name) {
        this.name = name;
        this.events = new ArrayList<>();
        // Placeholder event so scheduler loopIndex is always incremented
        this.events.add(new ScheduledNoteEvent(0L, NoteEvent.NOTHING, null, 0, 0, 0));
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

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_DB);
    }

    public List<ScheduledNoteEvent> getEvents() {
        return events;
    }

    public void prepareEventsDeepCopy() {
        eventsDeepCopy = events.toArray(new ScheduledNoteEvent[0]);
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
        if(isStarted) {
            long now = System.nanoTime();
            long phantomTicks = this.schedulerLoopIndex * (ticks - this.loopLengthTicks);
            checkpointTicks = getTicksAtTime(now) + phantomTicks;
            checkpointNanos = now;

            this.loopLengthTicks = ticks;
            backtrack(checkpointTicks);
        } else {
            this.loopLengthTicks = ticks;
        }
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
        if(isStarted && !isPaused) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now);
            checkpointNanos = now;
            backtrack(checkpointTicks);
        }
        nanosPerTick = updatedNanosPerTick;
    }

    public void addEvent(int insertIdx, ScheduledNoteEvent event) {
        event.patternId = this.id;
        event.id = nextEventId;
        nextEventId++;

        events.add(insertIdx, event);
        incrementEventIndex();
        backtrack(getTicksAtTime(System.nanoTime()));

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public void removeEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        boolean removed = events.remove(event);
        if(removed) {
            if(schedulerEventIndex >= events.size()) {
                schedulerEventIndex = events.size() - 1;
            }
            if(events.size() == 0) {
                schedulerEventIndex = 0;
            } else {
                backtrack(getTicksAtTime(System.nanoTime()));
            }
        }
    }

    public NoteEvent removeAndGetEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        boolean removed = events.remove(event);
        if(removed) {
            long ticksNow = getTicksAtTime(System.nanoTime());
            if(schedulerEventIndex >= events.size()) {
                schedulerEventIndex = events.size() - 1;
            }
            if(events.size() == 0) {
                schedulerEventIndex = 0;
            } else {
                backtrack(ticksNow);
            }
            if(getScheduledEventTicks(event) >= ticksNow) {
                return event.getNoteEvent(schedulerLoopIndex);
            } else {
                return event.getNoteEvent(schedulerLoopIndex + 1);
            }
        }
        return null;
    }

    public void start() {
        isStarted = true;
        isPaused = false;
        schedulerLoopIndex = 0;
        schedulerEventIndex = 0;
        checkpointNanos = System.nanoTime();
        checkpointTicks = 0;
    }

    public void pause() {
        long now = System.nanoTime();
        checkpointTicks = getTicksAtTime(now);
        checkpointNanos = now;

        backtrack(checkpointTicks);
        isPaused = true;
    }

    public void resume() {
        if(!isPaused) {
            return;
        }
        isPaused = false;
        checkpointNanos = System.nanoTime();
    }

    private void backtrack(long targetTicks) {
        // Backtrack to first eventIndex to be scheduled after resume/refresh
        if(events.size() == 0) {
            return;
        }

        boolean correctNextScheduledEventIndex = false;
        ScheduledNoteEvent n;
        while(!correctNextScheduledEventIndex) {
            decrementEventIndex();
            n = events.get(schedulerEventIndex);
            correctNextScheduledEventIndex = (getScheduledEventTicks(n) <= targetTicks);
        }
        incrementEventIndex();
        // loopResumePosition = System.nanoTime() - loopZeroStart;
    }

    private long getTicksAtTime(long time) {
        if(isPaused) {
            return checkpointTicks;
        }
        long afterCheckpointTicks = (long) ((time - checkpointNanos) / nanosPerTick);
        return checkpointTicks + afterCheckpointTicks;
    }

    private long getTimeAtTicks(long ticks) {
        return (long) ((ticks - checkpointTicks) * this.nanosPerTick + checkpointNanos);
    }

    private long getLoopLengthNanos() {
        return (long) (loopLengthTicks * nanosPerTick);
    }

    public long getScheduledEventTicks(ScheduledNoteEvent n) {
        return n.offsetTicks + this.schedulerLoopIndex * this.loopLengthTicks;
    }

    public long getScheduledEventTime(ScheduledNoteEvent n) {
        long fullOffsetTicks = getScheduledEventTicks(n);
        return getTimeAtTicks(fullOffsetTicks);
    }

    private void decrementEventIndex() {
        schedulerEventIndex--;
        if(schedulerEventIndex < 0) {
            schedulerLoopIndex--;
            schedulerEventIndex = events.size() - 1;
        }
    }

    private void incrementEventIndex() {
        schedulerEventIndex++;
        if(schedulerEventIndex >= events.size()) {
            schedulerLoopIndex++;
            schedulerEventIndex = 0;
        }
    }

    public long getTimeToNextEvent() {
        if(events.size() == 0) {
            return Long.MAX_VALUE;
        }
        return getScheduledEventTime(events.get(schedulerEventIndex)) - System.nanoTime();
    }

    public void getNextEvents(NoteEvent[] noteEventsOut, long tolerance) {
        long firstEventTime = Long.MIN_VALUE;

        int tmpL = schedulerLoopIndex;
        int tmpE = schedulerEventIndex;
        for(int i = 0; i < noteEventsOut.length; i++) {
            ScheduledNoteEvent current = events.get(schedulerEventIndex);
            long eventTime = getScheduledEventTime(current);

            if(i == 0) {
                firstEventTime = eventTime;
                noteEventsOut[i] = current.getNoteEvent(schedulerLoopIndex);
                incrementEventIndex();
            } else if(eventTime - firstEventTime < tolerance) {
                // setOnChangedListener multiple events within time frame
                noteEventsOut[i] = current.getNoteEvent(schedulerLoopIndex);
                incrementEventIndex();
            } else {
                Log.d("Pattern", String.format("Scheduled %d:%d to %d:%d", tmpL, tmpE,
                        schedulerLoopIndex, schedulerEventIndex));
                return;
            }
        }
    }

    public static Pattern getEmptyPattern() {
        Pattern retval = new Pattern("");
        retval.setLoopLengthTicks(new MusicTime(1, 0, 0).getTicks());
        retval.setTempo(140);
        return retval;
    }
}
