package libre.sampler.listeners;

import android.widget.NumberPicker;

public class StatefulScrollListener implements NumberPicker.OnScrollListener {
    private int scrollState;

    @Override
    public void onScrollStateChange(NumberPicker view, int newState) {
        this.scrollState = newState;
    }

    public int getScrollState() {
        return scrollState;
    }
}
