package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.Pattern;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.utils.PatternThread;

public class PatternBuilder {
    private PatternThread patternThread;
    private final Pattern pattern;
    private int nextNoteId = 0;

    private Consumer<List<ScheduledNoteEvent>> listener;

    public PatternBuilder(PatternThread patternThread, Pattern pattern) {
        this.patternThread = patternThread;
        this.pattern = pattern;
    }

    public void setOnChangedListener(Consumer<List<ScheduledNoteEvent>> listener) {
        patternThread.lock.lock();
        try {
            this.listener = listener;
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void add(ScheduledNoteEvent... noteEvents) {
        patternThread.lock.lock();
        try {
            for(ScheduledNoteEvent noteEvent : noteEvents) {
                int insertIdx = Collections.binarySearch(pattern.events, noteEvent, new Comparator<ScheduledNoteEvent>() {
                    @Override
                    public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                        return o1.offsetTicks.compareTo(o2.offsetTicks);
                    }
                });
                if(insertIdx < 0) {
                    insertIdx = -insertIdx - 1;
                }
                pattern.addEvent(insertIdx, noteEvent);
            }
            patternThread.notifyPatternsChanged();
            if(listener != null) {
                listener.accept(pattern.events);
            }
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void remove(ScheduledNoteEvent noteEventOn, ScheduledNoteEvent noteEventOff) {
        patternThread.lock.lock();
        try {
            int removeIdx = pattern.events.indexOf(noteEventOn);
            if(removeIdx != -1) {
                pattern.removeEvent(removeIdx);
                pattern.removeEventAfterNextSchedule(noteEventOff);
                patternThread.notifyPatternsChanged();
                if(listener != null) {
                    listener.accept(pattern.events);
                }
            }
        } finally {
            patternThread.lock.unlock();
        }
    }

    public int getNextNoteId() {
        return nextNoteId++;
    }
}
