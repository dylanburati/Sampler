package libre.sampler.models;

import java.util.List;

import libre.sampler.utils.AppConstants;

public class Pattern {
    public int projectId;
    public int id;
    public List<ScheduledNoteEvent> events;

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
    }

    public void setPatternId(int id) {
        this.id = id;
    }

    public synchronized void setLoopLengthTicks(long ticks) {
        this.loopLengthTicks = ticks;
        this.checkpointTicks = 0;
        this.checkpointNanos = getLastLoopStartTime();
        this.nextScheduledLoopIndex = 0;
    }

    public synchronized void setTempo(double bpm) {
        long updatedNanosPerTick = (long) (60 * 1e9 / (1.0 * AppConstants.TICKS_PER_BEAT * bpm));
        setNanosPerTick(updatedNanosPerTick);
    }

    public synchronized double getTempo() {
        if(nanosPerTick == 0) {
            return 0;
        }
        return (60 * 1e9 / (1.0 * AppConstants.TICKS_PER_BEAT * nanosPerTick));
    }

    public synchronized void setNanosPerTick(long updatedNanosPerTick) {
        if(isStarted && !isPaused) {
            long now = System.nanoTime();
            checkpointTicks = getTicksAtTime(now) - loopLengthTicks * this.nextScheduledLoopIndex;
            checkpointNanos = now;
            nextScheduledLoopIndex = 0;
        }
        nanosPerTick = updatedNanosPerTick;
    }

    public void start() {
        isStarted = true;
        nextScheduledLoopIndex = 0;
        nextScheduledEventIndex = 0;
        checkpointNanos = System.nanoTime();
        checkpointTicks = 0;
    }

    public synchronized void pause() {
        isPaused = true;
        checkpointTicks = getTicksAtTime(System.nanoTime());

        // Backtrack to first event to be scheduled after resume
        boolean correctNextScheduledEventIndex = false;
        ScheduledNoteEvent n;
        while(!correctNextScheduledEventIndex) {
            decrementEventIndex();
            n = events.get(this.nextScheduledEventIndex);
            correctNextScheduledEventIndex = (getScheduledEventTicks(n) <= checkpointTicks);
        }
        incrementEventIndex();
        // loopResumePosition = System.nanoTime() - loopZeroStart;
    }

    public synchronized void resume() {
        if(!isPaused) {
            return;
        }
        isPaused = false;
        checkpointNanos = System.nanoTime();
    }

    private synchronized long getTicksAtTime(long time) {
        long afterCheckpointTicks = (time - checkpointNanos) / nanosPerTick;
        return checkpointTicks + afterCheckpointTicks;
    }

    private synchronized long getTimeAtTicks(long ticks) {
        return (ticks - checkpointTicks) * this.nanosPerTick + checkpointNanos;
    }

    private synchronized long getLoopLengthNanos() {
        return loopLengthTicks * nanosPerTick;
    }

    public synchronized long getLastLoopStartTicks() {
        if(nanosPerTick == 0) {
            return 0;
        }
        int fullLoops = (int) ((System.nanoTime() - checkpointNanos) / getLoopLengthNanos());
        return checkpointTicks + fullLoops * loopLengthTicks;
    }

    public synchronized long getLastLoopStartTime() {
        if(nanosPerTick == 0) {
            return 0;
        }
        int fullLoops = (int) ((System.nanoTime() - checkpointNanos) / getLoopLengthNanos());
        return checkpointNanos + fullLoops * getLoopLengthNanos();
    }

    public synchronized long getScheduledEventTicks(ScheduledNoteEvent n) {
        return n.offsetTicks + this.nextScheduledLoopIndex * this.loopLengthTicks;
    }

    public synchronized long getScheduledEventTime(ScheduledNoteEvent n) {
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

        for(int i = 0; i < noteEventsOut.length; i++) {
            ScheduledNoteEvent current = events.get(this.nextScheduledEventIndex);
            long eventTime = getScheduledEventTime(current);

            if(i == 0) {
                waitNanos = eventTime - now;
                firstEventTime = eventTime;
                noteEventsOut[i] = current.event;
                incrementEventIndex();
            } else if(eventTime - firstEventTime < tolerance) {
                // add multiple events within time frame
                noteEventsOut[i] = current.event;
                incrementEventIndex();
            } else {
                return waitNanos;
            }
        }
        return waitNanos;
    }
}
