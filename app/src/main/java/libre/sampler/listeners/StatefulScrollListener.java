package libre.sampler.listeners;

import android.widget.NumberPicker;

public class StatefulScrollListener implements NumberPicker.OnScrollListener {
    public int scrollState;

    @Override
    public void onScrollStateChange(NumberPicker view, int scrollState) {
        this.scrollState = scrollState;
    }
}
