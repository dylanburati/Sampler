package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.models.PatternEvent;
import libre.sampler.utils.EventSource;

public class PatternEventSource implements EventSource<PatternEvent> {
    private final Object lock = new Object();

    private Map<String, Consumer<PatternEvent>> listeners = new HashMap<>();

    @Override
    public void add(String tag, Consumer<PatternEvent> listener) {
        synchronized(lock) {
            listeners.put(tag, listener);
        }
    }

    @Override
    public void dispatch(PatternEvent patternEvent) {
        synchronized(lock) {
            for(Consumer<PatternEvent> fn : listeners.values()) {
                fn.accept(patternEvent);
            }
        }
    }

    @Override
    public void dispatchTo(String tag, PatternEvent patternEvent) {
        Consumer<PatternEvent> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(patternEvent);
        }
    }
}