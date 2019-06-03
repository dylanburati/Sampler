package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.VisualNote;

public class PatternEditNoteProperties extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private Set<VisualNote> selectedNotes;

    private View rootView;
    private MusicTime inputNoteStart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_note_properties, container, false);
        selectedNotes = patternsFragment.getSelectedNotes();

        initNoteStartPickers();
        initNoteLengthPickers();

        patternsFragment.patternEditEventSource.add("NoteProperties", new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.SELECTED_NOTES)) {
                    updateSelectedLabel();
                    updateNoteStartPickers();
                    updateNoteLengthPickers();
                }
            }
        });
        updateSelectedLabel();

        rootView.findViewById(R.id.close_note_properties).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);
            }
        });

        return rootView;
    }

    private void initNoteStartPickers() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.note_start_picker_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.note_start_picker_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.note_start_picker_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.note_start_label).setVisibility(visibility);

        inputNoteStart = new MusicTime(0L);
        if(selectedNotes.size() > 0) {
            inputNoteStart.setTicks(selectedNotes.iterator().next().startTicks);
        }

        final StatefulScrollListener pickerUserTicksScrolling = new StatefulScrollListener();
        pickerUserTicks.setOnScrollListener(pickerUserTicksScrolling);
        pickerUserTicks.setMinValue(0);
        pickerUserTicks.setMaxValue(MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
        pickerUserTicks.setValue(inputNoteStart.userTicks);
        pickerUserTicks.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        pickerUserTicks.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputNoteStart.userTicks = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputNoteStart.sixteenths += 1;
                        pickerSixteenths.setValue(inputNoteStart.sixteenths);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerSixteenths.getValue() > pickerSixteenths.getMinValue()) {
                        inputNoteStart.sixteenths -= 1;
                        pickerSixteenths.setValue(inputNoteStart.sixteenths);
                    }
                }
                if(selectedNotes.size() > 0) {
                    patternsFragment.setNoteStart(inputNoteStart);
                }
            }
        });

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setValue(inputNoteStart.sixteenths);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputNoteStart.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputNoteStart.bars += 1;
                        pickerBars.setValue(inputNoteStart.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        inputNoteStart.bars -= 1;
                        pickerBars.setValue(inputNoteStart.bars);
                    }
                }
                if(selectedNotes.size() > 0) {
                    patternsFragment.setNoteStart(inputNoteStart);
                }
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setValue(inputNoteStart.bars);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputNoteStart.bars = newVal;
                if(selectedNotes.size() > 0) {
                    patternsFragment.setNoteStart(inputNoteStart);
                }
            }
        });

        pickerUserTicks.setWrapSelectorWheel(true);
        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        patternsFragment.patternEditEventSource.remove("NoteProperties");
    }

    private void initNoteLengthPickers() {
        int visibility = (selectedNotes.size() <= 1) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.note_length_picker_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.note_length_picker_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.note_length_picker_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.note_length_label).setVisibility(visibility);
        MusicTime initialLength = patternsFragment.getNoteLength();

        final StatefulScrollListener pickerUserTicksScrolling = new StatefulScrollListener();
        pickerUserTicks.setOnScrollListener(pickerUserTicksScrolling);
        pickerUserTicks.setMinValue(0);
        pickerUserTicks.setMaxValue(MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
        pickerUserTicks.setValue(initialLength.userTicks);
        pickerUserTicks.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        pickerUserTicks.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                MusicTime noteLength = patternsFragment.getNoteLength();
                noteLength.userTicks = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        noteLength.sixteenths += 1;
                        pickerSixteenths.setValue(noteLength.sixteenths);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerSixteenths.getValue() > pickerSixteenths.getMinValue()) {
                        noteLength.sixteenths -= 1;
                        pickerSixteenths.setValue(noteLength.sixteenths);
                    }
                }
                patternsFragment.setNoteLength(noteLength, true);
            }
        });

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setValue(initialLength.sixteenths);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                MusicTime noteLength = patternsFragment.getNoteLength();
                noteLength.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        noteLength.bars += 1;
                        pickerBars.setValue(noteLength.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        noteLength.bars -= 1;
                        pickerBars.setValue(noteLength.bars);
                    }
                }
                patternsFragment.setNoteLength(noteLength, true);
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setValue(initialLength.bars);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                MusicTime noteLength = patternsFragment.getNoteLength();
                noteLength.bars = newVal;
                patternsFragment.setNoteLength(noteLength, true);
            }
        });

        pickerUserTicks.setWrapSelectorWheel(true);
        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);
    }

    private void updateNoteStartPickers() {
        int visibility = (selectedNotes.size() <= 1) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.note_start_picker_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.note_start_picker_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.note_start_picker_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.note_start_label).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            inputNoteStart.setTicks(selectedNotes.iterator().next().startTicks);
            pickerBars.setValue(inputNoteStart.bars);
            pickerSixteenths.setValue(inputNoteStart.sixteenths);
            pickerUserTicks.setValue(inputNoteStart.userTicks);
        }
    }

    private void updateNoteLengthPickers() {
        int visibility = (selectedNotes.size() <= 1) ? View.VISIBLE : View.GONE;
        final NumberPicker pickerBars = rootView.findViewById(R.id.note_length_picker_bars);
        final NumberPicker pickerSixteenths = rootView.findViewById(R.id.note_length_picker_sixteenths);
        final NumberPicker pickerUserTicks = rootView.findViewById(R.id.note_length_picker_ticks);
        pickerBars.setVisibility(visibility);
        pickerSixteenths.setVisibility(visibility);
        pickerUserTicks.setVisibility(visibility);
        rootView.findViewById(R.id.note_length_label).setVisibility(visibility);

        if(selectedNotes.size() == 1) {
            MusicTime inputNoteLength = patternsFragment.getNoteLength();
            inputNoteLength.setTicks(selectedNotes.iterator().next().lengthTicks);
            pickerBars.setValue(inputNoteLength.bars);
            pickerSixteenths.setValue(inputNoteLength.sixteenths);
            pickerUserTicks.setValue(inputNoteLength.userTicks);
            patternsFragment.setNoteLength(inputNoteLength, false);
        } else if(selectedNotes.size() == 0) {
            MusicTime inputNoteLength = patternsFragment.getNoteLength();
            pickerBars.setValue(inputNoteLength.bars);
            pickerSixteenths.setValue(inputNoteLength.sixteenths);
            pickerUserTicks.setValue(inputNoteLength.userTicks);
        }
    }

    private void updateSelectedLabel() {
        int num = selectedNotes.size();
        String label = getResources().getQuantityString(R.plurals.selected_notes_label, num, num);
        ((TextView) rootView.findViewById(R.id.selected_notes_label)).setText(label);
    }
}
