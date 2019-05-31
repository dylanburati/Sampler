package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.utils.EventSource;

public class InstrumentEventSource implements EventSource<InstrumentEvent> {
    private Map<String, Consumer<InstrumentEvent>> listeners = new HashMap<>();

    @Override
    public void add(String tag, Consumer<InstrumentEvent> listener) {
        listeners.put(tag, listener);
    }

    @Override
    public void dispatch(InstrumentEvent event) {
        for(Consumer<InstrumentEvent> fn : listeners.values()) {
            fn.accept(event);
        }
    }

    @Override
    public void dispatchTo(String tag, InstrumentEvent event) {
        Consumer<InstrumentEvent> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(event);
        }
    }

    @Override
    public void remove(String tag) {
        listeners.remove(tag);
    }
}