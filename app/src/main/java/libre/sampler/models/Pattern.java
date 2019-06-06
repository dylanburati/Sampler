package libre.sampler.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;
import libre.sampler.utils.IdStatus;
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
    private int schedulerEventIndex;
    @Ignore
    private int schedulerLoopIndex;
    @Ignore
    private int numPendingEvents;

    @Ignore
    public boolean isStarted;
    @Ignore
    public boolean isPaused;

    @Ignore
    private final Object schedulerLock = new Object();

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
        synchronized(schedulerLock) {
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
        synchronized(schedulerLock) {
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
            synchronized(schedulerLock) {
                long now = System.nanoTime();
                checkpointTicks = getTicksAtTime(now);
                checkpointNanos = now;
                backtrack(checkpointTicks);
                nanosPerTick = updatedNanosPerTick;
            }
        } else {
            nanosPerTick = updatedNanosPerTick;
        }
    }

    public void addEvent(ScheduledNoteEvent noteEvent) {
        noteEvent.patternId = this.id;
        noteEvent.id = nextEventId;
        nextEventId++;

        synchronized(schedulerLock) {
            int insertIdx = Collections.binarySearch(events, noteEvent, new Comparator<ScheduledNoteEvent>() {
                @Override
                public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                    return Long.compare(o1.offsetTicks, o2.offsetTicks);
                }
            });
            if(insertIdx < 0) {
                insertIdx = -insertIdx - 1;
            }

            cancelPending();
            Log.d("Pattern", "addEvent " + insertIdx);
            if(insertIdx <= schedulerEventIndex) {
                schedulerEventIndex++;
            }
            events.add(insertIdx, noteEvent);
        }

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public boolean removeEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        synchronized(schedulerLock) {
            int removeIdx = events.indexOf(event);
            if(removeIdx != -1) {
                Log.d("Pattern", "removeEvent " + removeIdx);
                cancelPending();
                if(schedulerEventIndex == removeIdx) {
                    if(schedulerEventIndex == events.size() - 1) {
                        schedulerLoopIndex++;
                        schedulerEventIndex = 0;
                    }
                } else if(schedulerEventIndex > removeIdx) {
                    decrementEventIndex();
                }
                events.remove(removeIdx);
            }
            return (removeIdx != -1);
        }
    }

    public NoteEvent removeAndGetEvent(ScheduledNoteEvent event) {
        // Removes eventIndex and moves to next index according to time
        // Last scheduled events will be invalidated
        synchronized(schedulerLock) {
            int removeIdx = events.indexOf(event);
            NoteEvent nextEvent = null;
            if(removeIdx != -1) {
                Log.d("Pattern", "removeAndGetEvent " + removeIdx);
                cancelPending();
                if(schedulerEventIndex <= removeIdx) {
                    // this event has not been dispatched during the loop
                    nextEvent = event.getNoteEvent(schedulerLoopIndex);
                    if(schedulerEventIndex == removeIdx) {
                        if(schedulerEventIndex == events.size() - 1) {
                            schedulerLoopIndex++;
                            schedulerEventIndex = 0;
                        }
                    }
                } else {
                    // schedulerEventIndex > removeIdx: this event has been dispatched during the loop
                    decrementEventIndex();
                    nextEvent = event.getNoteEvent(schedulerLoopIndex + 1);
                }
                events.remove(removeIdx);
            }
            return nextEvent;
        }
    }

    public void start() {
        synchronized(schedulerLock) {
            isStarted = true;
            isPaused = false;
            schedulerLoopIndex = 0;
            schedulerEventIndex = 0;
            checkpointNanos = System.nanoTime();
            checkpointTicks = 0;
        }
    }

    public void pause() {
        synchronized(schedulerLock) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now);
            checkpointNanos = now;

            backtrack(checkpointTicks);
            isPaused = true;
        }
    }

    public void resume() {
        if(!isPaused) {
            return;
        }
        synchronized(schedulerLock) {
            isPaused = false;
            checkpointNanos = System.nanoTime();
        }
    }

    public void stop() {
        isStarted = false;
    }

    private void cancelPending() {
        synchronized(schedulerLock) {
            while(numPendingEvents > 0) {
                decrementEventIndex();
                numPendingEvents--;
            }
        }
    }

    public void confirmPending() {
        synchronized(schedulerLock) {
            numPendingEvents = 0;
        }
    }

    private void backtrack(long targetTicks) {
        // Backtrack to first eventIndex to be scheduled after resume/refresh
        if(events.size() == 0) {
            return;
        }

        synchronized(schedulerLock) {
            boolean correctNextScheduledEventIndex = false;
            ScheduledNoteEvent n;
            while(!correctNextScheduledEventIndex) {
                decrementEventIndex();
                n = events.get(schedulerEventIndex);
                correctNextScheduledEventIndex = (getScheduledEventTicks(n) <= targetTicks);
            }
            incrementEventIndex();
        }
        // loopResumePosition = System.nanoTime() - loopZeroStart;
    }

    private long getTicksAtTime(long time) {
        synchronized(schedulerLock) {
            if(isPaused) {
                return checkpointTicks;
            }
            long afterCheckpointTicks = (long) ((time - checkpointNanos) / nanosPerTick);
            return checkpointTicks + afterCheckpointTicks;
        }
    }

    private long getTimeAtTicks(long ticks) {
        synchronized(schedulerLock) {
            return (long) ((ticks - checkpointTicks) * this.nanosPerTick + checkpointNanos);
        }
    }

    private long getLoopLengthNanos() {
        synchronized(schedulerLock) {
            return (long) (loopLengthTicks * nanosPerTick);
        }
    }

    public long getScheduledEventTicks(ScheduledNoteEvent n) {
        synchronized(schedulerLock) {
            return n.offsetTicks + this.schedulerLoopIndex * this.loopLengthTicks;
        }
    }

    public long getScheduledEventTime(ScheduledNoteEvent n) {
        synchronized(schedulerLock) {
            long fullOffsetTicks = getScheduledEventTicks(n);
            return getTimeAtTicks(fullOffsetTicks);
        }
    }

    private void decrementEventIndex() {
        synchronized(schedulerLock) {
            schedulerEventIndex--;
            if(schedulerEventIndex < 0) {
                schedulerLoopIndex--;
                schedulerEventIndex = events.size() - 1;
            }
        }
    }

    private void incrementEventIndex() {
        synchronized(schedulerLock) {
            schedulerEventIndex++;
            if(schedulerEventIndex >= events.size()) {
                schedulerLoopIndex++;
                schedulerEventIndex = 0;
            }
        }
    }

    public long getTimeToNextEvent() {
        synchronized(schedulerLock) {
            if(events.size() == 0) {
                return Long.MAX_VALUE;
            }
            return getScheduledEventTime(events.get(schedulerEventIndex)) - System.nanoTime();
        }
    }

    public void getNextEvents(NoteEvent[] noteEventsOut, long tolerance) {
        synchronized(schedulerLock) {
            cancelPending();
            long firstEventTime = Long.MAX_VALUE;

            int tmpL = schedulerLoopIndex;
            int tmpE = schedulerEventIndex;
            for(int i = 0; i < noteEventsOut.length; i++) {
                ScheduledNoteEvent current = events.get(schedulerEventIndex);
                long eventTime = getScheduledEventTime(current);

                if(eventTime - firstEventTime < tolerance) {
                    if(i == 0) {
                        firstEventTime = eventTime;
                    }
                    noteEventsOut[i] = current.getNoteEvent(schedulerLoopIndex);
                    incrementEventIndex();
                    numPendingEvents++;
                } else {
                    Log.d("Pattern", String.format("Scheduled %d:%d to %d:%d", tmpL, tmpE,
                            schedulerLoopIndex, schedulerEventIndex));
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
