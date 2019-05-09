package libre.sampler.utils;

import androidx.arch.core.util.Function;

public interface Converter<I, O> {
    public O convert(I input);
}
