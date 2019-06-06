package libre.sampler.publishers;

import androidx.core.util.Consumer;
import libre.sampler.models.PatternEvent;

public class PatternEventSource extends MapEventSource<PatternEvent> {
    private final Object lock = new Object();

    @Override
    public void add(String tag, Consumer<PatternEvent> listener) {
        synchronized(lock) {
            super.add(tag, listener);
        }
    }

    @Override
    public void dispatch(PatternEvent patternEvent) {
        synchronized(lock) {
            super.dispatch(patternEvent);
        }
    }

    @Override
    public void remove(String tag) {
        synchronized(lock) {
            super.remove(tag);
        }
    }
}