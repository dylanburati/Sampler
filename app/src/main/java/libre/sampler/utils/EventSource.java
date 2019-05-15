package libre.sampler.utils;

import androidx.core.util.Consumer;

public interface EventSource<T> {
    public void add(String tag, Consumer<T> listener);
    public void dispatch(T event);
    public void dispatchTo(String tag, T event);
}
