package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;

public class PatternEditPatternLength extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private View rootView;
    private MusicTime inputLoopLength;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_pattern_length, container, false);
        ProjectViewModel viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        inputLoopLength = new MusicTime(viewModel.getPianoRollPattern().getLoopLengthTicks());
        initPickers();

        rootView.findViewById(R.id.submit_pattern_length).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setPatternLength(inputLoopLength);
            }
        });

        rootView.findViewById(R.id.close_pattern_length).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);
            }
        });

        return rootView;
    }

    private void initPickers() {
        final NumberPicker pickerBars = rootView.findViewById(R.id.pattern_length_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.pattern_length_sixteenths);

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setValue(inputLoopLength.sixteenths);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputLoopLength.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputLoopLength.bars += 1;
                        pickerBars.setValue(inputLoopLength.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        inputLoopLength.bars -= 1;
                        pickerBars.setValue(inputLoopLength.bars);
                    }
                }
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setValue(inputLoopLength.bars);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputLoopLength.bars = newVal;
            }
        });

        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);
    }
}
