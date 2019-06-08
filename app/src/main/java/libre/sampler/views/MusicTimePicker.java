package libre.sampler.views;

import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.utils.Debouncer;
import libre.sampler.utils.MusicTime;

import static android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_IDLE;

public abstract class MusicTimePicker {
    private static final int MAX_INPUT_BARS = 999;
    private static final int SIXTEENTHS_WRAP_THRESHOLD = 2;
    private static final int USER_TICKS_WRAP_THRESHOLD = 2;
    @NonNull
    private final NumberPicker pickerBars;
    @NonNull
    private final NumberPicker pickerSixteenths;
    @Nullable
    private final NumberPicker pickerUserTicks;
    
    private final StatefulScrollListener pickerSixteenthsScroll = new StatefulScrollListener();
    private final StatefulScrollListener pickerUserTicksScroll = new StatefulScrollListener();

    private final AlternatingFormatterProvider sixteenthsFormatters = new AlternatingFormatterProvider("%d");
    private final AlternatingFormatterProvider userTicksFormatters = new AlternatingFormatterProvider("%02d");

    private final MusicTime value = new MusicTime(0L);
    private Debouncer debouncer = new Debouncer();

    public MusicTimePicker(@NonNull NumberPicker pickerBars, @NonNull NumberPicker pickerSixteenths) {
        this.pickerBars = pickerBars;
        this.pickerSixteenths = pickerSixteenths;
        this.pickerUserTicks = null;
        attachInternalListeners();
    }
    
    public MusicTimePicker(@NonNull NumberPicker pickerBars, @NonNull NumberPicker pickerSixteenths,
                           @NonNull NumberPicker pickerUserTicks) {
        this.pickerBars = pickerBars;
        this.pickerSixteenths = pickerSixteenths;
        this.pickerUserTicks = pickerUserTicks;
        attachInternalListeners();
    }
    
    private void attachInternalListeners() {
        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldBars, int newBars) {
                value.bars = newBars;
                setWrapPickerSixteenths(true);
                setWrapPickerUserTicks(true);
                MusicTimePicker.this.onValueChanged(value);
            }
        });
        pickerBars.setWrapSelectorWheel(false);

        pickerSixteenths.setOnScrollListener(pickerSixteenthsScroll);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setFormatter(sixteenthsFormatters.getNext());
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldSixteenths, int newSixteenths) {
                value.sixteenths = newSixteenths;
                boolean rolloverMinusAllowed = (value.bars > 0);
                if(pickerSixteenthsScroll.scrollState != SCROLL_STATE_IDLE) {
                    if(oldSixteenths == picker.getMaxValue() && newSixteenths == picker.getMinValue()) {
                        // rollover +
                        value.changeByTicks(MusicTime.TICKS_PER_BAR);
                        pickerBars.setValue(value.bars);
                    } else if(oldSixteenths == picker.getMinValue() && newSixteenths == picker.getMaxValue()) {
                        if(rolloverMinusAllowed) {
                            // rollover -
                            value.changeByTicks(-MusicTime.TICKS_PER_BAR);
                            pickerBars.setValue(value.bars);
                        } else {
                            // cancel
                            value.sixteenths = oldSixteenths;
                            picker.setValue(oldSixteenths);
                        }
                    }
                }
                setWrapPickerSixteenths(true);
                setWrapPickerUserTicks(true);
                MusicTimePicker.this.onValueChanged(value);
            }
        });
        pickerSixteenths.setWrapSelectorWheel(false);

        if(pickerUserTicks != null) {
            pickerUserTicks.setOnScrollListener(pickerUserTicksScroll);
            pickerUserTicks.setMinValue(0);
            pickerUserTicks.setMaxValue(MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
            pickerUserTicks.setFormatter(sixteenthsFormatters.getNext());
            pickerUserTicks.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldUserTicks, int newUserTicks) {
                    value.userTicks = newUserTicks;
                    boolean rolloverMinusAllowed = (value.bars > 0) || (value.sixteenths > 0);
                    if(pickerUserTicksScroll.scrollState != SCROLL_STATE_IDLE) {
                        if(oldUserTicks == picker.getMaxValue() && newUserTicks == picker.getMinValue()) {
                            // rollover +
                            value.changeByTicks(MusicTime.TICKS_PER_SIXTEENTH);
                            pickerSixteenths.setValue(value.sixteenths);
                            pickerBars.setValue(value.bars);
                        } else if(oldUserTicks == picker.getMinValue() && newUserTicks == picker.getMaxValue()) {
                            if(rolloverMinusAllowed) {
                                // rollover -
                                value.changeByTicks(-MusicTime.TICKS_PER_SIXTEENTH);
                                pickerSixteenths.setValue(value.sixteenths);
                                pickerBars.setValue(value.bars);
                            } else {
                                // cancel
                                value.userTicks = oldUserTicks;
                                picker.setValue(oldUserTicks);
                            }
                        }
                    }
                    setWrapPickerUserTicks(true);
                    MusicTimePicker.this.onValueChanged(value);
                }
            });
            pickerUserTicks.setWrapSelectorWheel(false);
        }
    }

    private void setWrapPickerSixteenths(final boolean redraw) {
        final boolean shouldWrap = (value.bars > 0 || value.sixteenths >= SIXTEENTHS_WRAP_THRESHOLD);
        final String taskName = "setWrapPickerSixteenths";
        final int debounceId = debouncer.getNextId(taskName);

        pickerSixteenths.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(debouncer.getCurrentId(taskName) != debounceId) {
                    return;
                }
                pickerSixteenths.setWrapSelectorWheel(shouldWrap);
                if(redraw) {
                    pickerSixteenths.setFormatter(sixteenthsFormatters.getNext());
                    pickerSixteenths.invalidate();
                }
            }
        }, 30);
    }

    private void setWrapPickerUserTicks(final boolean redraw) {
        if(pickerUserTicks == null) {
            return;
        }
        final boolean shouldWrap = (value.bars > 0 || value.sixteenths > 0 ||
                value.userTicks >= USER_TICKS_WRAP_THRESHOLD);

        final String taskName = "setWrapPickerUserTicks";
        final int debounceId = debouncer.getNextId(taskName);

        pickerUserTicks.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(debouncer.getCurrentId(taskName) != debounceId) {
                    return;
                }
                pickerUserTicks.setWrapSelectorWheel(shouldWrap);
                if(redraw) {
                    pickerUserTicks.setFormatter(userTicksFormatters.getNext());
                    pickerUserTicks.invalidate();
                }
            }
        }, 30);
    }

    public void setVisibility(int visibility) {
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        if(pickerUserTicks != null) {
            pickerUserTicks.setVisibility(visibility);
        }
    }

    public void setValue(MusicTime newVal) {
        setTicks(newVal.getTicks());
    }

    public void setTicks(long newValInTicks) {
        value.setTicks(newValInTicks);
        pickerBars.setValue(value.bars);

        pickerSixteenths.setValue(value.sixteenths);
        setWrapPickerSixteenths(true);

        if(pickerUserTicks != null) {
            pickerUserTicks.setValue(value.userTicks);
            setWrapPickerUserTicks(true);
        }
    }

    public abstract void onValueChanged(MusicTime value);

    public MusicTime getValue() {
        return value;
    }

    private static class AlternatingFormatterProvider {
        private final NumberPicker.Formatter formatter0;
        private final NumberPicker.Formatter formatter1;
        private int nextIdx = 0;

        // Hack to get NumberPicker to redraw without changing value
        public AlternatingFormatterProvider(final String formatString) {
            formatter0 = new NumberPicker.Formatter() {
                @Override
                public String format(int value) {
                    return String.format(formatString, value);
                }
            };

            formatter1 = new NumberPicker.Formatter() {
                @Override
                public String format(int value) {
                    return String.format(formatString, value);
                }
            };
        }

        public NumberPicker.Formatter getNext() {
            NumberPicker.Formatter f;
            if(nextIdx % 2 == 0) {
                f = formatter0;
            } else {
                f = formatter1;
            }

            nextIdx++;
            return f;
        }
    }
}
