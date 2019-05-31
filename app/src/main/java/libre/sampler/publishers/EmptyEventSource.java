package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.utils.EventSource;

public class EmptyEventSource implements EventSource<String> {
    private Map<String, Consumer<String>> listeners = new HashMap<>();

    @Override
    public void add(String tag, Consumer<String> listener) {
        listeners.put(tag, listener);
    }

    @Override
    public void dispatch(String eventName) {
        for(Consumer<String> fn : listeners.values()) {
            fn.accept(eventName);
        }
    }

    @Override
    public void dispatchTo(String tag, String eventName) {
        Consumer<String> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(eventName);
        }
    }

    @Override
    public void remove(String tag) {
        listeners.remove(tag);
    }
}
