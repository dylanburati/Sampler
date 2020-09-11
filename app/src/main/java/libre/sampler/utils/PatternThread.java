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

    protected final Lock lock = new ReentrantLock();
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
        for(Pattern p : runningPatterns.values()) {
            p.stop();
        }
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
        if(isSuspended) {
            return;
        }

        isSuspended = true;
        for(Pattern p : runningPatterns.values()) {
            p.pause();
        }
        notifyPatternsChanged();
    }

    public void resumeLoop() {
        if(!isSuspended) {
            return;
        }

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
                    if(!p.isPlaying()) {
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
                    // awaiting allows lock to be used by other thread
                    while(minWaitTime > NANOS_FOR_WAIT && runningPatternsModCount == lastModCount) {
                        long t0 = System.nanoTime();
                        patternsChangedTrigger.awaitNanos(minWaitTime);
                        minWaitTime -= System.nanoTime() - t0;
                    }
                }
            } catch(InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }

            if(done) return;
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
            } else if(runningPatternsModCount == lastModCount) {
                lock.lock();
                try {
                    nextPattern.confirmEvents();
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
            }
        }
    }

    public Editor getEditor(Pattern pattern) {
        return new Editor(this, pattern);
    }

    public static class Editor implements AutoCloseable {
        private final PatternThread thread;
        private final boolean isPatternActive;
        public final Pattern pattern;

        public Editor(PatternThread patternThread, Pattern pattern) {
            this.thread = patternThread;
            this.pattern = pattern;
            this.isPatternActive = thread.runningPatterns.containsValue(pattern);

            if(isPatternActive) {
                thread.lock.lock();
            }
        }

        @Override
        public void close() {
            if(isPatternActive) {
                thread.notifyPatternsChanged();
                thread.lock.unlock();
            }
        }
    }
}
