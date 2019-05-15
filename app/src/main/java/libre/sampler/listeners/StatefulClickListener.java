package libre.sampler.listeners;

import android.view.View;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulClickListener <T> extends SingleStateHolder<T> implements View.OnClickListener {
    public StatefulClickListener(T data) {
        super(data);
    }
}
