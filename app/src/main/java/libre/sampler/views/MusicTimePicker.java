package libre.sampler.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import libre.sampler.R;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.ViewUtil;

import static android.widget.NumberPicker.OnScrollListener.SCROLL_STATE_IDLE;

public class MusicTimePicker extends RelativeLayout {
    public static final int DEFAULT_PICKER_WIDTH_DP = 64;
    public static final int DEFAULT_PICKER_HEIGHT_DP = 60;

    private int pickerWidth;
    private int pickerHeight;

    public interface OnValueChangedListener {
        void onValueChange(MusicTime value);
    }

    private final MusicTime value = new MusicTime(0L);
    private final MusicTime maxValue = new MusicTime(999, 0, 0);

    private OnValueChangedListener externalListener;

    private static final int PICKER_INDEX_BARS = 0;
    private static final int PICKER_INDEX_SIXTEENTHS = 1;
    private static final int PICKER_INDEX_USER_TICKS = 2;

    private int[] PICKER_INDICES;
    private final NumberPicker[] pickers;
    private final StatefulScrollListener[] scrollListeners;
    private AlternatingFormatterProvider[] formatterProviders = new AlternatingFormatterProvider[] {
            new AlternatingFormatterProvider("%d"),
            new AlternatingFormatterProvider("%d"),
            new AlternatingFormatterProvider("%2d")
    };

    public MusicTimePicker(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        float density = displayMetrics.density;
        pickerWidth = ViewUtil.dpToPxSize(density, DEFAULT_PICKER_WIDTH_DP);
        pickerHeight = ViewUtil.dpToPxSize(density, DEFAULT_PICKER_HEIGHT_DP);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MusicTimePicker, 0, 0);
        int numPickers = a.getInt(R.styleable.MusicTimePicker_num_pickers, 3);
        if(numPickers < 3) {
            PICKER_INDICES = new int[]{ 0, 1 };
        } else {
            PICKER_INDICES = new int[]{ 0, 1, 2 };
        }
        a.recycle();
        scrollListeners = new StatefulScrollListener[PICKER_INDICES.length];
        pickers = new NumberPicker[PICKER_INDICES.length];

        for(int i : PICKER_INDICES) {
            LayoutParams pickerLp = new LayoutParams(pickerWidth, LayoutParams.MATCH_PARENT);
            pickers[i] = new NumberPicker(context);
            pickers[i].setId(View.generateViewId());

            if(i == 0) {
                pickerLp.addRule(ALIGN_PARENT_LEFT);
            } else {
                pickerLp.addRule(RIGHT_OF, pickers[i - 1].getId());
                pickerLp.addRule(ALIGN_TOP, pickers[i - 1].getId());
            }

            addView(pickers[i], pickerLp);
        }

        setPickerFormatters();
        attachInternalListeners();
        setValue(value);
        setMaxValue(maxValue);
    }

    private void setPickerFormatters() {
        for(int i : PICKER_INDICES) {
            pickers[i].setFormatter(formatterProviders[i].getNext());
        }
    }

    private long lastRolloverMs = 0;
    private final MusicTime tmpValue = new MusicTime(0L);
    private void attachInternalListeners() {
        for(int i : PICKER_INDICES) {
            scrollListeners[i] = new StatefulScrollListener();
            pickers[i].setOnScrollListener(scrollListeners[i]);
        }

        pickers[PICKER_INDEX_BARS].setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                tmpValue.set(value);
                tmpValue.bars = newVal;
                setValue(tmpValue, PICKER_INDEX_BARS, true);
            }
        });

        pickers[PICKER_INDEX_SIXTEENTHS].setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                tmpValue.set(value);
                tmpValue.sixteenths = newVal;
                if((System.currentTimeMillis() - lastRolloverMs) < 100 ||
                        scrollListeners[PICKER_INDEX_SIXTEENTHS].getScrollState() != SCROLL_STATE_IDLE) {

                    lastRolloverMs = System.currentTimeMillis();
                    if(oldVal == picker.getMaxValue() && newVal == 0) {
                        tmpValue.changeByTicks(MusicTime.TICKS_PER_BAR);
                    } else if(oldVal == 0 && newVal == picker.getMaxValue()) {
                        tmpValue.changeByTicks(-MusicTime.TICKS_PER_BAR);
                    }
                }
                setValue(tmpValue, PICKER_INDEX_SIXTEENTHS, true);
            }
        });

        if(pickers.length > PICKER_INDEX_USER_TICKS) {
            pickers[PICKER_INDEX_USER_TICKS].setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                @Override
                public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                    tmpValue.set(value);
                    tmpValue.userTicks = newVal;
                    if((System.currentTimeMillis() - lastRolloverMs) < 100 ||
                            scrollListeners[PICKER_INDEX_USER_TICKS].getScrollState() != SCROLL_STATE_IDLE) {

                        lastRolloverMs = System.currentTimeMillis();
                        if(oldVal == picker.getMaxValue() && newVal == 0) {
                            tmpValue.changeByTicks(MusicTime.TICKS_PER_SIXTEENTH);
                        } else if(oldVal == 0 && newVal == picker.getMaxValue()) {
                            tmpValue.changeByTicks(-MusicTime.TICKS_PER_SIXTEENTH);
                        }
                    }
                    setValue(tmpValue, PICKER_INDEX_USER_TICKS, true);
                }
            });
        }
    }

    private boolean isInitialSetValue = true;
    public void setValue(MusicTime v) {
        setValue(v, -1, false);
    }

    private void setValue(MusicTime v, int skipIndex, boolean dispatchChange) {
        if(skipIndex != PICKER_INDEX_BARS && (isInitialSetValue || v.bars != value.bars)) {
            pickers[PICKER_INDEX_BARS].setValue(v.bars);
        }
        if(skipIndex != PICKER_INDEX_SIXTEENTHS && (isInitialSetValue || v.sixteenths != value.sixteenths)) {
            pickers[PICKER_INDEX_SIXTEENTHS].setValue(v.sixteenths);
        }
        if(pickers.length > PICKER_INDEX_USER_TICKS && skipIndex != PICKER_INDEX_USER_TICKS &&
                (isInitialSetValue || v.userTicks != value.userTicks)) {
            pickers[PICKER_INDEX_USER_TICKS].setValue(v.userTicks);
        }
        isInitialSetValue = false;

        value.set(v);
        updateWrapEnabled();
        if(dispatchChange && externalListener != null) {
            externalListener.onValueChange(value);
        }
    }

    public MusicTime getValue() {
        return value;
    }

    public void setMaxValue(MusicTime v) {
        maxValue.set(v);
        if(value.getTicks() > maxValue.getTicks()) {
            setValue(v);
        } else {
            updateWrapEnabled();
        }
    }

    private void updateWrapEnabled() {
        boolean[] willDisableDecrement = new boolean[3];
        boolean[] willDisableIncrement = new boolean[3];
        if(value.bars == 0) {
            willDisableDecrement[PICKER_INDEX_SIXTEENTHS] = (value.sixteenths < 3);
            if(value.sixteenths == 0) {
                willDisableDecrement[PICKER_INDEX_USER_TICKS] = (value.userTicks < 3);
            }
        }

        if(value.bars == maxValue.bars) {
            willDisableIncrement[PICKER_INDEX_SIXTEENTHS] = ((maxValue.sixteenths - value.sixteenths) < 3);
            if(value.sixteenths == maxValue.sixteenths) {
                willDisableIncrement[PICKER_INDEX_USER_TICKS] = ((maxValue.sixteenths - value.sixteenths) < 3);
            }
        }

        pickers[PICKER_INDEX_BARS].setWrapSelectorWheel(false);
        pickers[PICKER_INDEX_BARS].setMaxValue(maxValue.bars);
        for(int i : new int[]{ PICKER_INDEX_SIXTEENTHS, PICKER_INDEX_USER_TICKS }) {
            if(i >= pickers.length) {
                break;
            }
            int normalMaxVal = (i == PICKER_INDEX_SIXTEENTHS ? MusicTime.SIXTEENTHS_PER_BAR - 1 : MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
            int nowrapMaxVal = (i == PICKER_INDEX_SIXTEENTHS ? maxValue.sixteenths : maxValue.bars);
            pickers[i].setWrapSelectorWheel(!willDisableDecrement[i] && !willDisableIncrement[i]);
            if(willDisableIncrement[i]) {
                pickers[i].setMaxValue(nowrapMaxVal);
            } else {
                pickers[i].setMaxValue(normalMaxVal);
            }
            pickers[i].setFormatter(formatterProviders[i].getNext());
        }
    }

    public void setOnValueChangedListener(OnValueChangedListener listener) {
        this.externalListener = listener;
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
