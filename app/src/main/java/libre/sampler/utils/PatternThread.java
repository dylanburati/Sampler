package libre.sampler.utils;

import android.os.Parcelable;

import java.util.Arrays;
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
    private NoteEventSource noteEventSource;
    public Map<String, Pattern> runningPatterns;
    private long runningPatternsModCount = 0;

    private static final int EVENT_GROUP_SIZE = 4;

    private Lock lock = new ReentrantLock();
    private Condition patternsChangedTrigger = lock.newCondition();
    private Condition suspendTrigger = lock.newCondition();
    private boolean done = false;
    private boolean suspended = false;

    public PatternThread(NoteEventSource noteEventSource) {
        this.noteEventSource = noteEventSource;
        this.runningPatterns = new HashMap<>();
    }
    
    public void addPattern(String tag, Pattern p) {
        runningPatternsModCount++;
        runningPatterns.put(tag, p);
        p.start();
        if(suspended) {
            p.pause();
        }

        notifyPatternsChanged();
    }

    public void clearPatterns() {
        runningPatternsModCount++;
        runningPatterns.clear();

        notifyPatternsChanged();
    }

    public void notifyPatternsChanged() {
        lock.lock();
        try {
            patternsChangedTrigger.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void suspendLoop() {
        suspended = true;
        for(Pattern p : runningPatterns.values()) {
            p.pause();
        }
        notifyPatternsChanged();
    }

    public void resumeLoop() {
        suspended = false;
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
        if(suspended) {
            suspended = false;
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
            NoteEvent[] noteEvents = new NoteEvent[EVENT_GROUP_SIZE];
            NoteEvent[] noteEventsMin = new NoteEvent[EVENT_GROUP_SIZE];
            long minWaitTime = Long.MAX_VALUE;
            long lastModCount = runningPatternsModCount;

            for(Pattern p : runningPatterns.values()) {
                if(p.events.isEmpty() || p.isPaused) {
                    continue;
                }

                Arrays.fill(noteEvents, null);
                long waitTime = p.getNextEvents(noteEvents, 2 * NANOS_PER_MILLI);
                if(waitTime < minWaitTime) {
                    nextPattern = p;
                    minWaitTime = waitTime;
                    System.arraycopy(noteEvents, 0, noteEventsMin, 0, EVENT_GROUP_SIZE);
                }
            }

            if(minWaitTime > NANOS_FOR_WAIT) {
                lock.lock();
                try {
                    // awaiting allows lock to be used by other thread
                    while(minWaitTime > NANOS_FOR_WAIT) {
                        long t0 = System.nanoTime();
                        patternsChangedTrigger.awaitNanos(minWaitTime);
                        if(runningPatternsModCount > lastModCount) {
                            // stop waiting for `next` if patterns added or removed
                            noteEventsMin = null;
                            break;
                        }
                        minWaitTime -= System.nanoTime() - t0;
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }

            if(suspended) {
                lock.lock();
                try {
                    // awaiting allows lock to be used by other thread
                    while(suspended) {
                        suspendTrigger.awaitNanos(Long.MAX_VALUE);
                    }
                } catch(InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            } else if(!done && noteEventsMin != null) {
                for(NoteEvent e : noteEventsMin) {
                    if(e != null) {
                        noteEventSource.dispatch(e);
                    } else {
                        break;
                    }
                }
            }
        }
    }
}
