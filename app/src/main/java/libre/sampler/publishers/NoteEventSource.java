package libre.sampler.publishers;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;

public class NoteEventSource extends MapEventSource<NoteEvent> {
    private final Object lock = new Object();

    @Override
    public void add(String tag, Consumer<NoteEvent> listener) {
        synchronized(lock) {
            super.add(tag, listener);
        }
    }

    @Override
    public void dispatch(NoteEvent noteEvent) {
        synchronized(lock) {
            super.dispatch(noteEvent);
        }
    }

    @Override
    public void remove(String tag) {
        synchronized(lock) {
            super.remove(tag);
        }
    }
}