package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.Instrument;
import libre.sampler.models.NoteEvent;
import libre.sampler.utils.EventSource;

public class InstrumentEventSource implements EventSource<Instrument> {
    private List<String> keys = new ArrayList<>();
    private List<Consumer<Instrument>> listeners = new ArrayList<>();

    public InstrumentEventSource() {
    }

    @Override
    public void add(String tag, Consumer<Instrument> listener) {
        int replaceIndex = (tag == null ? -1 : keys.indexOf(tag));
        if(replaceIndex == -1) {
            keys.add(tag);
            listeners.add(listener);
        } else {
            listeners.set(replaceIndex, listener);
        }
    }

    @Override
    public void dispatch(Instrument instrument) {
        for(Consumer<Instrument> fn : listeners) {
            fn.accept(instrument);
        }
    }
}