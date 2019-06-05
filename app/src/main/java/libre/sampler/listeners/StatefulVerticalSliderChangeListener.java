package libre.sampler.listeners;

import libre.sampler.utils.SingleStateHolder;
import libre.sampler.views.VerticalSlider;

public abstract class StatefulVerticalSliderChangeListener<T> extends SingleStateHolder<T> implements VerticalSlider.OnProgressChangedListener {
    public StatefulVerticalSliderChangeListener(T data) {
        super(data);
    }
}
