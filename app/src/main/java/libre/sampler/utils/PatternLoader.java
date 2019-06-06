package libre.sampler.utils;

import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;

public class PatternLoader {
    private final PatternThread patternThread;

    public PatternLoader(PatternThread patternThread) {
        this.patternThread = patternThread;
    }

    public void addToPattern(Pattern pattern, ScheduledNoteEvent... noteEvents) {
        patternThread.lock.lock();
        try {
            for(ScheduledNoteEvent noteEvent : noteEvents) {
                pattern.addEvent(noteEvent);
            }
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void removeFromPattern(Pattern pattern, ScheduledNoteEvent noteEventOn, ScheduledNoteEvent noteEventOff) {
        patternThread.lock.lock();
        try {
            boolean hasEvent = pattern.removeEvent(noteEventOn);
            if(hasEvent) {
                NoteEvent sendOff = pattern.removeAndGetEvent(noteEventOff);
                if(sendOff != null) {
                    patternThread.noteEventSource.dispatch(sendOff);
                }
                patternThread.notifyPatternsChanged();
            }
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void setLoopLength(Pattern pattern, MusicTime loopLength) {
        patternThread.lock.lock();
        try {
            pattern.setLoopLengthTicks(loopLength.getTicks());
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void setTempo(Pattern pattern, double bpm) {
        patternThread.lock.lock();
        try {
            pattern.setTempo(bpm);
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }
}
