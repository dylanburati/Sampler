package libre.sampler.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.NoteEventSource;

import static libre.sampler.utils.AppConstants.NANOS_PER_MILLI;

public class PatternThread extends Thread {
    private NoteEventSource noteEventSource;
    private List<PatternInfo> runningPatterns;
    private long runningPatternsLastModified = 0;

    private Lock lock = new ReentrantLock();
    private Condition lockCondition = lock.newCondition();
    private boolean done = false;

    public PatternThread(NoteEventSource noteEventSource) {
        this.noteEventSource = noteEventSource;
        this.runningPatterns = new ArrayList<>();
    }
    
    public int addPattern(List<ScheduledNoteEvent> events, long loopLength) {
        runningPatternsLastModified = System.nanoTime();
        PatternInfo p = new PatternInfo(events, loopLength);
        p.loopZeroStart = System.nanoTime();
        p.id = runningPatterns.size();
        runningPatterns.add(p);

        lock.lock();
        try {
            lockCondition.signalAll();
        } finally {
            lock.unlock();
        }
        return p.id;
    }

    public void clearPatterns() {
        runningPatternsLastModified = System.nanoTime();
        runningPatterns.clear();

        lock.lock();
        try {
            lockCondition.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void finish() {
        done = true;
    }

    @Override
    public void run() {
        while(!done) {
            PatternInfo nextPattern = null;
            int[] eventIndices = null;
            long soonest = Long.MAX_VALUE;
            long lastMod = runningPatternsLastModified;

            for(PatternInfo p : runningPatterns) {
                if(p.events.isEmpty()) {
                    continue;
                }

                long pLastLoopStart = p.getLastLoopStart();  // save before updating in getNextEventsIndices
                int[] pIndices = p.getNextEventsIndices(2 * NANOS_PER_MILLI);
                long pSoonest = p.events.get(pIndices[0]).offsetNanos + pLastLoopStart;

                if(pSoonest < soonest) {
                    nextPattern = p;
                    eventIndices = pIndices;
                    soonest = pSoonest;
                } else if(pSoonest - System.nanoTime() <= 2 * NANOS_PER_MILLI) {
                    for(int i = pIndices[0]; i < pIndices[1]; i++) {
                        noteEventSource.dispatch(p.events.get(i % p.eventCount).event);
                    }
                }
            }

            long waitTime = soonest - System.nanoTime();
            if(waitTime > 2 * NANOS_PER_MILLI) {
                lock.lock();
                try {
                    // awaiting allows lock to be used by other thread
                    while(waitTime > 2 * NANOS_PER_MILLI) {
                        lockCondition.awaitNanos(waitTime);
                        if(runningPatternsLastModified > lastMod) {
                            // stop waiting for `next` if patterns added or removed
                            eventIndices = null;
                            break;
                        }
                        waitTime = soonest - System.nanoTime();
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
            
            if(!done && eventIndices != null) {
                for(int i = eventIndices[0]; i < eventIndices[1]; i++) {
                    noteEventSource.dispatch(nextPattern.events.get(i % nextPattern.eventCount).event);
                }
            }
        }
    }

    private class PatternInfo {
        public Integer id;
        public long loopLength;
        private int loopIndex = 0;
        public long loopZeroStart;

        public List<ScheduledNoteEvent> events;
        private int eventIndex = 0;
        public int eventCount;

        public PatternInfo(List<ScheduledNoteEvent> events, long loopLength) {
            this.events = events;
            this.eventCount = events.size();
            this.loopLength = loopLength;
        }

        public long getLastLoopStart() {
            return loopZeroStart + (loopIndex * loopLength);
        }

        public int[] getNextEventsIndices(long tolerance) {
            int[] indices = new int[2];
            indices[0] = indices[1] = this.eventIndex;

            boolean toleranceExceeded = false;
            long nextOffset = Long.MIN_VALUE;
            while(!toleranceExceeded) {
                ScheduledNoteEvent n = events.get(this.eventIndex);
                if(indices[0] == indices[1]) {
                    nextOffset = n.offsetNanos;
                } else {
                    toleranceExceeded = (n.offsetNanos - nextOffset) > tolerance;
                }

                if(!toleranceExceeded) {
                    this.eventIndex++;
                    indices[1]++;  // exclusive
                    if(this.eventIndex >= this.eventCount) {
                        nextOffset -= loopLength;
                        loopIndex++;
                        this.eventIndex = 0;
                    }
                }
            }
            return indices;
        }
    }
}
