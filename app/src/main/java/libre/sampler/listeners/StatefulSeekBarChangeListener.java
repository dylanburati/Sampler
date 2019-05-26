package libre.sampler.listeners;

import android.widget.SeekBar;

import libre.sampler.utils.SingleStateHolder;

public abstract class StatefulSeekBarChangeListener<T> extends SingleStateHolder<T> implements SeekBar.OnSeekBarChangeListener {
    public boolean isTrackingTouch;

    public StatefulSeekBarChangeListener(T data) {
        super(data);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isTrackingTouch = false;
    }
}
