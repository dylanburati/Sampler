package libre.sampler.listeners;

import android.view.View;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulTouchListener<T> extends SingleStateHolder<T> implements View.OnTouchListener {
    public StatefulTouchListener(T data) {
        super(data);
    }
}
