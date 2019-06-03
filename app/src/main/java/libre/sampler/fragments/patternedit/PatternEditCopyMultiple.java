package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.VisualNote;

public class PatternEditCopyMultiple extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private TreeSet<VisualNote> selectedNotes;
    private ProjectViewModel viewModel;

    private static final MusicTime[] COMMON_INTERVALS = new MusicTime[]{
            new MusicTime(0, 0, 12),
            new MusicTime(0, 0, 16),
            new MusicTime(0, 1, 0),
            new MusicTime(0, 1, 8),
            new MusicTime(0, 2, 0),
            new MusicTime(0, 4, 0),
            new MusicTime(1, 0, 0)
    };
    
    private View rootView;
    private MusicTime inputInterval;
    private int inputCount;
    private boolean commonIntervalChosen = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_copy_multiple, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);
        selectedNotes = patternsFragment.getSelectedNotes();

        initIntervalPickers();
        initCountPicker();

        patternsFragment.patternEditEventSource.add("CopyMultiple", new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.SELECTED_NOTES)) {
                    updateStatus();
                    updateIntervalPickers();
                    updateMaxCount();
                    updateStatus();
                }
            }
        });
        updateStatus();

        rootView.findViewById(R.id.submit_copy_multiple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedNotes.size() > 0) {
                    for(VisualNote n : selectedNotes) {
                        patternsFragment.insertMultiplePianoRollNotes(n, inputInterval, inputCount);
                    }
                }
            }
        });

        rootView.findViewById(R.id.close_copy_multiple).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);
            }
        });

        return rootView;
    }

    private void initIntervalPickers() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.interval_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.interval_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.interval_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.interval_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_picker).setVisibility(visibility);

        inputInterval = new MusicTime(0L);
        if(selectedNotes.size() > 0) {
            chooseCommonInterval();
            commonIntervalChosen = true;
        }

        final StatefulScrollListener pickerUserTicksScrolling = new StatefulScrollListener();
        pickerUserTicks.setOnScrollListener(pickerUserTicksScrolling);
        pickerUserTicks.setMinValue(0);
        pickerUserTicks.setMaxValue(MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
        pickerUserTicks.setValue(inputInterval.userTicks);
        pickerUserTicks.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        pickerUserTicks.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputInterval.userTicks = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputInterval.sixteenths += 1;
                        pickerSixteenths.setValue(inputInterval.sixteenths);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerSixteenths.getValue() > pickerSixteenths.getMinValue()) {
                        inputInterval.sixteenths -= 1;
                        pickerSixteenths.setValue(inputInterval.sixteenths);
                    }
                }
                updateMaxCount();
                updateStatus();
            }
        });

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setValue(inputInterval.sixteenths);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputInterval.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputInterval.bars += 1;
                        pickerBars.setValue(inputInterval.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        inputInterval.bars -= 1;
                        pickerBars.setValue(inputInterval.bars);
                    }
                }
                updateMaxCount();
                updateStatus();
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setValue(inputInterval.bars);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputInterval.bars = newVal;
                updateMaxCount();
                updateStatus();
            }
        });

        pickerUserTicks.setWrapSelectorWheel(true);
        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);
    }

    private void chooseCommonInterval() {
        long selectionLengthTicks = getSelectionLengthTicks();
        int commonSel = 0;
        for(/* commonSel = 0 */; commonSel < COMMON_INTERVALS.length - 1; commonSel++) {
            if(COMMON_INTERVALS[commonSel].getTicks() >= selectionLengthTicks) {
                break;
            }
        }
        inputInterval.setTicks(COMMON_INTERVALS[commonSel].getTicks());
    }

    private long getSelectionStartTicks() {
        if(selectedNotes.size() == 0) {
            return 0;
        }
        return selectedNotes.first().startTicks;
    }

    private long getSelectionEndTicks() {
        long end = 0;
        for(VisualNote n : selectedNotes) {
            long noteEnd = n.startTicks + n.lengthTicks;
            if(noteEnd > end) {
                end = noteEnd;
            }
        }
        return end;
    }

    private long getSelectionLengthTicks() {
        return getSelectionEndTicks() - getSelectionStartTicks();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        patternsFragment.patternEditEventSource.remove("CopyMultiple");
    }

    private void initCountPicker() {
        final NumberPicker countPicker = rootView.findViewById(R.id.count_picker);
        countPicker.setMinValue(0);
        countPicker.setMaxValue(MAX_INPUT_BARS);
        countPicker.setValue(1);
        countPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputCount = newVal;
                updateStatus();
            }
        });

        countPicker.setWrapSelectorWheel(false);
    }

    private void updateIntervalPickers() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.interval_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.interval_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.interval_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.interval_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_picker).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            if(!commonIntervalChosen) {
                chooseCommonInterval();
                commonIntervalChosen = true;
                pickerBars.setValue(inputInterval.bars);
                pickerSixteenths.setValue(inputInterval.sixteenths);
                pickerUserTicks.setValue(inputInterval.userTicks);
            }
        }
    }

    private void updateMaxCount() {
        int maxCount = MAX_INPUT_BARS;
        if(selectedNotes.size() > 0) {
            long intervalTicks = inputInterval.getTicks();
            long maxTicks = viewModel.getPianoRollPattern().getLoopLengthTicks() - getSelectionStartTicks();
            maxCount = (int) (maxTicks / intervalTicks) - 1;
            long leftoverTicks = maxTicks - maxCount * intervalTicks;
            if(leftoverTicks < getSelectionLengthTicks()) {
                maxCount += 1;
            }
        }

        inputCount = Math.min(inputCount, maxCount);
        final NumberPicker countPicker = rootView.findViewById(R.id.count_picker);
        countPicker.setMaxValue(maxCount);
    }

    private void updateStatus() {
        if(selectedNotes.size() > 0) {
            long startTicks = getSelectionStartTicks();
            long endTicks = startTicks + inputInterval.getTicks() * (inputCount + 1);
            String status = getResources().getString(R.string.copy_multiple_status_range,
                    MusicTime.ticksToString(startTicks), MusicTime.ticksToString(endTicks));
            ((TextView) rootView.findViewById(R.id.copy_multiple_status)).setText(status);
        } else {
            ((TextView) rootView.findViewById(R.id.copy_multiple_status)).setText(R.string.nothing_selected);
        }
    }
}
