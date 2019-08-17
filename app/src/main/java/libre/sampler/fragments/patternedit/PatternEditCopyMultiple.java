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
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.MusicTimePicker;
import libre.sampler.views.VisualNote;

public class PatternEditCopyMultiple extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    public static final String TAG = "CopyMultiple";
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
    private int inputCount;
    private boolean commonIntervalChosen = false;
    private MusicTimePicker intervalPicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_copy_multiple, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);
        selectedNotes = patternsFragment.getSelectedNotes();

        initIntervalPicker();
        initCountPicker();

        patternsFragment.patternEditEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.SELECTED_NOTES)) {
                    updateIntervalPicker();
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
                    patternsFragment.copyMultiplePianoRollNotes(intervalPicker.getValue(), inputCount);
                }
            }
        });

        return rootView;
    }

    private void initIntervalPicker() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        intervalPicker = rootView.findViewById(R.id.interval_picker);
        intervalPicker.setOnValueChangedListener(new MusicTimePicker.OnValueChangedListener() {
            @Override
            public void onValueChange(MusicTime value) {
                updateMaxCount();
                updateStatus();
            }
        });
        intervalPicker.setVisibility(visibility);
        rootView.findViewById(R.id.interval_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_picker).setVisibility(visibility);
        rootView.findViewById(R.id.submit_copy_multiple).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            chooseCommonInterval();
            commonIntervalChosen = true;
        }
    }

    private void chooseCommonInterval() {
        long selectionLengthTicks = getSelectionLengthTicks();
        int commonSel = 0;
        for(/* commonSel = 0 */; commonSel < COMMON_INTERVALS.length - 1; commonSel++) {
            if(COMMON_INTERVALS[commonSel].getTicks() >= selectionLengthTicks) {
                break;
            }
        }
        intervalPicker.setValue(COMMON_INTERVALS[commonSel]);
        updateMaxCount();
        updateStatus();
    }

    private long getSelectionStartTicks() {
        if(selectedNotes.size() == 0) {
            return 0;
        }
        return selectedNotes.first().getStartTicks();
    }

    private long getSelectionEndTicks() {
        long end = 0;
        for(VisualNote n : selectedNotes) {
            long noteEnd = n.getEndTicks();
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
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        super.onDestroyView();
        this.rootView = null;
        this.intervalPicker = null;
        patternsFragment.patternEditEventSource.remove(TAG);
    }

    private void initCountPicker() {
        final NumberPicker countPicker = rootView.findViewById(R.id.count_picker);
        countPicker.setMinValue(0);
        updateMaxCount();
        inputCount = Math.min(1, countPicker.getMaxValue());
        countPicker.setValue(inputCount);
        countPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputCount = newVal;
                updateStatus();
            }
        });

        countPicker.setWrapSelectorWheel(false);
    }

    private void updateIntervalPicker() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        intervalPicker.setVisibility(visibility);
        rootView.findViewById(R.id.interval_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_label).setVisibility(visibility);
        rootView.findViewById(R.id.count_picker).setVisibility(visibility);
        rootView.findViewById(R.id.submit_copy_multiple).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            if(!commonIntervalChosen) {
                chooseCommonInterval();
                commonIntervalChosen = true;
            }
        }
    }

    private void updateMaxCount() {
        int maxCount = MAX_INPUT_BARS;
        if(selectedNotes.size() > 0) {
            long intervalTicks = intervalPicker.getValue().getTicks();
            long maxTicks = viewModel.getPianoRollPattern().getLoopLengthTicks() - getSelectionStartTicks();
            maxCount = (int) (maxTicks / intervalTicks) - 1;
            long leftoverTicks = maxTicks - (maxCount + 1) * intervalTicks;
            if(leftoverTicks >= getSelectionLengthTicks()) {
                maxCount += 1;
            }
        }

        maxCount = Math.max(0, maxCount);
        inputCount = Math.min(inputCount, maxCount);
        final NumberPicker countPicker = rootView.findViewById(R.id.count_picker);
        countPicker.setMaxValue(maxCount);
    }

    private void updateStatus() {
        if(selectedNotes.size() > 0) {
            long startTicks = getSelectionStartTicks();
            long endTicks = startTicks + intervalPicker.getValue().getTicks() * (inputCount + 1);
            String status = getResources().getString(R.string.copy_multiple_status_range,
                    MusicTime.ticksToString(startTicks), MusicTime.ticksToString(endTicks));
            ((TextView) rootView.findViewById(R.id.copy_multiple_status)).setText(status);
        } else {
            ((TextView) rootView.findViewById(R.id.copy_multiple_status)).setText(R.string.nothing_selected);
        }
    }
}
