package libre.sampler.listeners;

import android.text.Editable;
import android.text.TextWatcher;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulTextWatcher<T> extends SingleStateHolder<T> implements TextWatcher {
    public StatefulTextWatcher(T data) {
        super(data);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }
}
