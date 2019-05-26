package libre.sampler.utils;

import java.util.Collections;
import java.util.Comparator;

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
                int insertIdx = Collections.binarySearch(pattern.getEvents(), noteEvent, new Comparator<ScheduledNoteEvent>() {
                    @Override
                    public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                        return Long.compare(o1.offsetTicks, o2.offsetTicks);
                    }
                });
                if(insertIdx < 0) {
                    insertIdx = -insertIdx - 1;
                }
                pattern.addEvent(insertIdx, noteEvent);
            }
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void removeFromPattern(Pattern pattern, ScheduledNoteEvent noteEventOn, ScheduledNoteEvent noteEventOff) {
        patternThread.lock.lock();
        try {
            pattern.removeEvent(noteEventOn);
            NoteEvent sendOff = pattern.removeAndGetEvent(noteEventOff);
            if(sendOff != null) {
                patternThread.noteEventSource.dispatch(sendOff);
            }
            patternThread.notifyPatternsChanged();
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
