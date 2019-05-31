package libre.sampler.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.publishers.NoteEventSource;

import static libre.sampler.utils.AppConstants.NANOS_PER_MILLI;

public class PatternThread extends Thread {
    private static final long NANOS_FOR_WAIT = 2 * NANOS_PER_MILLI;
    public NoteEventSource noteEventSource;
    public Map<String, Pattern> runningPatterns;
    private long runningPatternsModCount = 0;

    private static final int EVENT_GROUP_SIZE = 4;

    public final Lock lock = new ReentrantLock();
    private Condition patternsChangedTrigger = lock.newCondition();
    private Condition suspendTrigger = lock.newCondition();
    private boolean done = false;
    public boolean isSuspended = false;

    public PatternThread(NoteEventSource noteEventSource) {
        this.noteEventSource = noteEventSource;
        this.runningPatterns = new HashMap<>();
    }

    public void addPattern(String tag, Pattern p) {
        runningPatterns.put(tag, p);
        p.start();
        if(isSuspended) {
            p.pause();
        }

        notifyPatternsChanged();
    }

    public void clearPatterns() {
        runningPatterns.clear();

        notifyPatternsChanged();
    }

    public void notifyPatternsChanged() {
        runningPatternsModCount++;
        lock.lock();
        try {
            patternsChangedTrigger.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void suspendLoop() {
        isSuspended = true;
        for(Pattern p : runningPatterns.values()) {
            p.pause();
        }
        notifyPatternsChanged();
    }

    public void resumeLoop() {
        isSuspended = false;
        for(Pattern p : runningPatterns.values()) {
            p.resume();
        }
        lock.lock();
        try {
            suspendTrigger.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void finish() {
        done = true;
        clearPatterns();
        if(isSuspended) {
            isSuspended = false;
            lock.lock();
            try {
                suspendTrigger.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    @Override
    public void run() {
        while(!done) {
            Pattern nextPattern = null;
            NoteEvent[] noteEventsOut = new NoteEvent[EVENT_GROUP_SIZE];
            long minWaitTime = Long.MAX_VALUE;
            long lastModCount = runningPatternsModCount;

            lock.lock();
            try {
                for(Pattern p : runningPatterns.values()) {
                    if(!p.isStarted || p.isPaused) {
                        continue;
                    }

                    long waitTime = p.getTimeToNextEvent();
                    if(waitTime < minWaitTime) {
                        nextPattern = p;
                        minWaitTime = waitTime;
                    }
                }

                if(nextPattern != null) {
                    // pattern updates position and writes events to `noteEventsOut`
                    nextPattern.getNextEvents(noteEventsOut, NANOS_FOR_WAIT);
                }

                if(minWaitTime > NANOS_FOR_WAIT) {
                    // Log.d("PatternThread", "Waiting " + (minWaitTime / 1e9) + " seconds");
                    // awaiting allows lock to be used by other thread
                    while(minWaitTime > NANOS_FOR_WAIT) {
                        long t0 = System.nanoTime();
                        patternsChangedTrigger.awaitNanos(minWaitTime);
                        if(runningPatternsModCount > lastModCount) {
                            // stop waiting for `next` if patterns added or removed
                            noteEventsOut = null;
                            break;
                        }
                        minWaitTime -= System.nanoTime() - t0;
                    }

                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

            if(isSuspended) {
                lock.lock();
                try {
                    // awaiting allows lock to be used by other thread
                    while(isSuspended) {
                        suspendTrigger.awaitNanos(Long.MAX_VALUE);
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            } else if(!done && runningPatternsModCount == lastModCount && noteEventsOut != null) {
                lock.lock();
                try {
                    nextPattern.confirmPending();
                    for(NoteEvent e : noteEventsOut) {
                        if(e != null) {
                            noteEventSource.dispatch(e);
                        } else {
                            break;
                        }
                    }
                } finally {
                    lock.unlock();
                }
            } else {
                // Log.d("PatternThread", "Skipped");
            }
        }
    }
}
