package libre.sampler.publishers;

import java.util.HashMap;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.utils.EventSource;

public class NoteEventSource implements EventSource<NoteEvent> {
    private final Object lock = new Object();

    private Map<String, Consumer<NoteEvent>> listeners = new HashMap<>();

    @Override
    public void add(String tag, Consumer<NoteEvent> listener) {
        synchronized(lock) {
            listeners.put(tag, listener);
        }
    }

    @Override
    public void dispatch(NoteEvent noteEvent) {
        synchronized(lock) {
            for(Consumer<NoteEvent> fn : listeners.values()) {
                fn.accept(noteEvent);
            }
        }
    }

    @Override
    public void dispatchTo(String tag, NoteEvent noteEvent) {
        Consumer<NoteEvent> fn = listeners.get(tag);
        if(fn != null) {
            fn.accept(noteEvent);
        }
    }
}