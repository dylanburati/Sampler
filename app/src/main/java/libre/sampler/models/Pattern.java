package libre.sampler.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.utils.AppConstants;

public class Pattern {
    public int projectId;
    public int id;
    public List<ScheduledNoteEvent> events;
    private List<ScheduledNoteEvent> eventsToRemoveAfterNextSchedule;

    private long nanosPerTick;
    private long loopLengthTicks;

    private long checkpointTicks;
    private long checkpointNanos;

    private int nextScheduledLoopIndex = 0;
    private int nextScheduledEventIndex = 0;
    public boolean isStarted;
    public boolean isPaused;

    public Pattern(List<ScheduledNoteEvent> events) {
        this.events = events;
        // Placeholder event so scheduler loop is not ignored
        events.add(0, new ScheduledNoteEvent(0L, NoteEvent.NOTHING, 0, 0, 0));
        this.eventsToRemoveAfterNextSchedule = new ArrayList<>(2);
    }

    public void setPatternId(int id) {
        this.id = id;
    }

    // All of the following methods should be called
    // by a thread holding the patternThread.lock
    public void setLoopLengthTicks(long ticks) {
        this.loopLengthTicks = ticks;
        if(isStarted) {
            this.checkpointTicks = 0;
            this.checkpointNanos = getLastLoopStartTime();
            this.nextScheduledLoopIndex = 0;
        }
    }

    public void setTempo(double bpm) {
        long updatedNanosPerTick = (long) (60 * 1e9 / (1.0 * AppConstants.TICKS_PER_BEAT * bpm));
        setNanosPerTick(updatedNanosPerTick);
    }

    public double getTempo() {
        if(nanosPerTick == 0) {
            return 0;
        }
        return (60 * 1e9 / (1.0 * AppConstants.TICKS_PER_BEAT * nanosPerTick));
    }

    public void setNanosPerTick(long updatedNanosPerTick) {
        if(isStarted && !isPaused) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now) - loopLengthTicks * this.nextScheduledLoopIndex;
            checkpointNanos = now;
            nextScheduledLoopIndex = 0;
        }
        nanosPerTick = updatedNanosPerTick;
    }

    public void addEvent(int insertIdx, ScheduledNoteEvent event) {
        events.add(insertIdx, event);
        incrementEventIndex();
        backtrack(getTicksAtTime(System.nanoTime()));
    }

    public void removeEvent(int removeIdx) {
        // Removes event and moves to next index according to time
        // Last scheduled events will be invalidated
        events.remove(removeIdx);
        if(nextScheduledEventIndex >= events.size()) {
            nextScheduledEventIndex = events.size() - 1;
        }
        if(events.size() == 0) {
            nextScheduledEventIndex = 0;
        } else {
            backtrack(getTicksAtTime(System.nanoTime()));
        }
    }

    public void removeEventInPlace(int removeIdx) {
        // Removes event and moves to next index according to scheduler
        events.remove(removeIdx);
        if(nextScheduledEventIndex >= events.size()) {
            nextScheduledLoopIndex++;
            nextScheduledEventIndex = 0;
        }
    }

    public void removeEventAfterNextSchedule(ScheduledNoteEvent noteEventOff) {
        eventsToRemoveAfterNextSchedule.add(noteEventOff);
    }

    public void start() {
        isStarted = true;
        nextScheduledLoopIndex = 0;
        nextScheduledEventIndex = 0;
        checkpointNanos = System.nanoTime();
        checkpointTicks = 0;
    }

    public void pause() {
        isPaused = true;
        checkpointTicks = getTicksAtTime(System.nanoTime());

        backtrack(checkpointTicks);
    }

    public void resume() {
        if(!isPaused) {
            return;
        }
        isPaused = false;
        checkpointNanos = System.nanoTime();
    }

    private void backtrack(long targetTicks) {
        // Backtrack to first event to be scheduled after resume/refresh
        if(events.size() == 0) {
            return;
        }

        boolean correctNextScheduledEventIndex = false;
        ScheduledNoteEvent n;
        while(!correctNextScheduledEventIndex) {
            decrementEventIndex();
            n = events.get(this.nextScheduledEventIndex);
            correctNextScheduledEventIndex = (getScheduledEventTicks(n) <= targetTicks);
        }
        incrementEventIndex();
        // loopResumePosition = System.nanoTime() - loopZeroStart;
    }

    private long getTicksAtTime(long time) {
        long afterCheckpointTicks = (time - checkpointNanos) / nanosPerTick;
        return checkpointTicks + afterCheckpointTicks;
    }

    private long getTimeAtTicks(long ticks) {
        return (ticks - checkpointTicks) * this.nanosPerTick + checkpointNanos;
    }

    private long getLoopLengthNanos() {
        return loopLengthTicks * nanosPerTick;
    }

    public void jumpToCorrectLoopIndex() {
        // Used after multiple empty loops
        if(nanosPerTick == 0) {
            return;
        }
        nextScheduledLoopIndex = (int) ((System.nanoTime() - checkpointNanos) / getLoopLengthNanos());
    }

    public long getLastLoopStartTime() {
        if(nanosPerTick == 0) {
            return 0;
        }
        int fullLoops = (int) ((System.nanoTime() - checkpointNanos) / getLoopLengthNanos());
        return checkpointNanos + fullLoops * getLoopLengthNanos();
    }

    public long getScheduledEventTicks(ScheduledNoteEvent n) {
        return n.offsetTicks + this.nextScheduledLoopIndex * this.loopLengthTicks;
    }

    public long getScheduledEventTime(ScheduledNoteEvent n) {
        long fullOffsetTicks = getScheduledEventTicks(n);
        return getTimeAtTicks(fullOffsetTicks);
    }

    private void decrementEventIndex() {
        this.nextScheduledEventIndex--;
        if(this.nextScheduledEventIndex < 0) {
            nextScheduledLoopIndex--;
            this.nextScheduledEventIndex = events.size() - 1;
        }
    }

    private void incrementEventIndex() {
        this.nextScheduledEventIndex++;
        if(this.nextScheduledEventIndex >= events.size()) {
            nextScheduledLoopIndex++;
            this.nextScheduledEventIndex = 0;
        }
    }

    public long getNextEvents(NoteEvent[] noteEventsOut, long tolerance) {
        long now = System.nanoTime();
        long firstEventTime = Long.MIN_VALUE;
        long waitNanos = Long.MAX_VALUE;

        String log = String.format("Scheduled %d:%d to ", this.nextScheduledLoopIndex, this.nextScheduledEventIndex);
        for(int i = 0; i < noteEventsOut.length; i++) {
            ScheduledNoteEvent current = events.get(this.nextScheduledEventIndex);
            long eventTime = getScheduledEventTime(current);

            if(i == 0) {
                waitNanos = eventTime - now;
                firstEventTime = eventTime;
                noteEventsOut[i] = current.getNoteEvent(nextScheduledLoopIndex);
                if(eventsToRemoveAfterNextSchedule.remove(current)) {
                    removeEventInPlace(this.nextScheduledEventIndex);
                } else {
                    incrementEventIndex();
                }
            } else if(eventTime - firstEventTime < tolerance) {
                // setOnChangedListener multiple events within time frame
                noteEventsOut[i] = current.getNoteEvent(nextScheduledLoopIndex);
                if(eventsToRemoveAfterNextSchedule.remove(current)) {
                    removeEventInPlace(this.nextScheduledEventIndex);
                } else {
                    incrementEventIndex();
                }
            } else {
                log += String.format("%d:%d", this.nextScheduledLoopIndex, this.nextScheduledEventIndex);
                Log.d("Pattern", log);
                return waitNanos;
            }
        }
        return waitNanos;
    }
}
