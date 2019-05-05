package libre.sampler.publishers;

import java.util.ArrayList;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.PatternEvent;
import libre.sampler.utils.EventSource;

public class PatternEventSource implements EventSource<PatternEvent> {
    private final Object lock = new Object();

    private List<String> keys = new ArrayList<>();
    private List<Consumer<PatternEvent>> listeners = new ArrayList<>();

    public PatternEventSource() {
    }

    @Override
    public void add(String tag, Consumer<PatternEvent> listener) {
        synchronized(lock) {
            int replaceIndex = (tag == null ? -1 : keys.indexOf(tag));
            if(replaceIndex == -1) {
                keys.add(tag);
                listeners.add(listener);
            } else {
                listeners.set(replaceIndex, listener);
            }
        }
    }

    @Override
    public void dispatch(PatternEvent patternEvent) {
        synchronized(lock) {
            for(Consumer<PatternEvent> fn : listeners) {
                fn.accept(patternEvent);
            }
        }
    }
}