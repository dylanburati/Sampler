package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.utils.EventSource;

public class NoteEventSource implements EventSource<NoteEvent> {
    private List<Consumer<NoteEvent>> listeners = new ArrayList<>();

    public NoteEventSource() {
    }

    @Override
    public void add(Consumer<NoteEvent> listener) {
        listeners.add(listener);
    }

    @Override
    public void dispatch(NoteEvent noteEvent) {
        for(Consumer<NoteEvent> fn : listeners) {
            fn.accept(noteEvent);
        }
    }
}