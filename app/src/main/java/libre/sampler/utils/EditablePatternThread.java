package libre.sampler.utils;

import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.NoteEventSource;

public class EditablePatternThread extends PatternThread {
    public EditablePatternThread(NoteEventSource noteEventSource) {
        super(noteEventSource);
    }

    public void addToPattern(Pattern pattern, ScheduledNoteEvent... noteEvents) {
        lock.lock();
        try {
            for(ScheduledNoteEvent noteEvent : noteEvents) {
                pattern.addEvent(noteEvent);
            }
            notifyPatternsChanged();
        } finally {
            lock.unlock();
        }
    }

    public void removeFromPattern(Pattern pattern, ScheduledNoteEvent noteEventOn, ScheduledNoteEvent noteEventOff) {
        lock.lock();
        try {
            boolean hasEvent = pattern.removeEvent(noteEventOn);
            if(hasEvent) {
                NoteEvent sendOff = pattern.removeAndGetEvent(noteEventOff);
                if(sendOff != null) {
                    noteEventSource.dispatch(sendOff);
                }
                notifyPatternsChanged();
            }
        } finally {
            lock.unlock();
        }
    }

    public void setLoopLength(Pattern pattern, MusicTime loopLength) {
        lock.lock();
        try {
            pattern.setLoopLengthTicks(loopLength.getTicks());
            notifyPatternsChanged();
        } finally {
            lock.unlock();
        }
    }

    public void setTempo(Pattern pattern, double bpm) {
        lock.lock();
        try {
            pattern.setTempo(bpm);
            notifyPatternsChanged();
        } finally {
            lock.unlock();
        }
    }
}
