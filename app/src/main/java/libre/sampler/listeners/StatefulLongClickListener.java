package libre.sampler.listeners;

import android.view.View;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulLongClickListener<T> extends SingleStateHolder<T> implements View.OnLongClickListener {
    public StatefulLongClickListener(T data) {
        super(data);
    }
}
