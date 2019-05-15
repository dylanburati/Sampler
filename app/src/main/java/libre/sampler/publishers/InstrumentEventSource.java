package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.models.Instrument;
import libre.sampler.utils.EventSource;

public class InstrumentEventSource implements EventSource<Instrument> {
    private Map<String, Consumer<Instrument>> listeners = new HashMap<>();

    public InstrumentEventSource() {
    }

    @Override
    public void add(String tag, Consumer<Instrument> listener) {
        listeners.put(tag, listener);
    }

    @Override
    public void dispatch(Instrument instrument) {
        for(Consumer<Instrument> fn : listeners.values()) {
            fn.accept(instrument);
        }
    }

    @Override
    public void dispatchTo(String tag, Instrument instrument) {
        Consumer<Instrument> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(instrument);
        }
    }
}