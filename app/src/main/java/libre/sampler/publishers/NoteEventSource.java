package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.utils.EventSource;

public class NoteEventSource implements EventSource<NoteEvent> {
    private List<String> keys = new ArrayList<>();
    private List<Consumer<NoteEvent>> listeners = new ArrayList<>();

    public NoteEventSource() {
    }

    @Override
    public void add(String tag, Consumer<NoteEvent> listener) {
        int replaceIndex = (tag == null ? -1 : keys.indexOf(tag));
        if(replaceIndex == -1) {
            keys.add(tag);
            listeners.add(listener);
        } else {
            listeners.set(replaceIndex, listener);
        }
    }

    @Override
    public void dispatch(NoteEvent noteEvent) {
        for(Consumer<NoteEvent> fn : listeners) {
            fn.accept(noteEvent);
        }
    }
}