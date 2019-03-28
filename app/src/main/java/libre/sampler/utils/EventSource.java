package libre.sampler.utils;

import androidx.core.util.Consumer;

public interface EventSource<T> {
    public void add(Consumer<T> listener);
    public void dispatch(T event);
}
