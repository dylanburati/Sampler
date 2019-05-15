package libre.sampler.listeners;

import android.widget.NumberPicker;

import libre.sampler.utils.SingleStateHolder;

public class StatefulScrollListener implements NumberPicker.OnScrollListener {
    public int scrollState;

    @Override
    public void onScrollStateChange(NumberPicker view, int scrollState) {
        this.scrollState = scrollState;
    }
}
