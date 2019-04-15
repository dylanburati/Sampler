package libre.sampler.listeners;

import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulTextWatcher<T> extends SingleStateHolder<T> implements TextWatcher {
    public StatefulTextWatcher(T data) {
        super(data);
    }
}
