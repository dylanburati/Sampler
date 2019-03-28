package libre.sampler.listeners;

import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.util.Log;
import android.view.View;

import libre.sampler.models.Project;
import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulClickListener <T> extends SingleStateHolder<T> implements View.OnClickListener {
    public StatefulClickListener(T data) {
        super(data);
    }
}
