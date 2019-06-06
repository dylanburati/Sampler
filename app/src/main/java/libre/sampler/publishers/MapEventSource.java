package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;

public class MapEventSource<T> {
    Map<String, Consumer<T>> listeners = new HashMap<>();

    public void add(String tag, Consumer<T> listener) {
        listeners.put(tag, listener);
    }

    public void dispatch(T event) {
        for(Consumer<T> fn : listeners.values()) {
            fn.accept(event);
        }
    }

    public void remove(String tag) {
        listeners.remove(tag);
    }
}
