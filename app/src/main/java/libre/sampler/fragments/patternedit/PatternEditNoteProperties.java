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
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.MusicTimePicker;
import libre.sampler.views.VisualNote;

public class PatternEditNoteProperties extends Fragment {
    public static final String TAG = "NoteProperties";
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private TreeSet<VisualNote> selectedNotes;

    private View rootView;
    private MusicTimePicker noteStartPicker;
    private MusicTimePicker noteLengthPicker;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_note_properties, container, false);
        selectedNotes = patternsFragment.getSelectedNotes();

        initNoteStartPicker();
        initNoteLengthPicker();

        patternsFragment.patternEditEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.SELECTED_NOTES)) {
                    updateSelectedLabel();
                    updateNoteStartPicker();
                    updateNoteLengthPicker();
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

    private void initNoteStartPicker() {
        noteStartPicker = new MusicTimePicker((NumberPicker) rootView.findViewById(R.id.note_start_picker_bars),
                (NumberPicker) rootView.findViewById(R.id.note_start_picker_sixteenths),
                (NumberPicker) rootView.findViewById(R.id.note_start_picker_ticks)) {
            @Override
            public void onValueChanged(MusicTime value) {
                if(selectedNotes.size() > 0) {
                    patternsFragment.setNoteStart(value);
                }
            }
        };

        updateNoteStartPicker();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        patternsFragment.patternEditEventSource.remove(TAG);
    }

    private void initNoteLengthPicker() {
        noteLengthPicker = new MusicTimePicker((NumberPicker) rootView.findViewById(R.id.note_length_picker_bars),
                (NumberPicker) rootView.findViewById(R.id.note_length_picker_sixteenths),
                (NumberPicker) rootView.findViewById(R.id.note_length_picker_ticks)) {
            @Override
            public void onValueChanged(MusicTime value) {
                patternsFragment.setNoteLength(value, true);
            }
        };

        updateNoteLengthPicker();
    }

    private void updateNoteStartPicker() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        noteStartPicker.setVisibility(visibility);
        rootView.findViewById(R.id.note_start_label).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            noteStartPicker.setTicks(selectedNotes.first().startTicks);
        }
    }

    private void updateNoteLengthPicker() {
        MusicTime inputNoteLength = patternsFragment.getInputNoteLength();
        noteLengthPicker.setValue(inputNoteLength);
    }

    private void updateSelectedLabel() {
        int num = selectedNotes.size();
        String label = getResources().getQuantityString(R.plurals.selected_notes_label, num, num);
        ((TextView) rootView.findViewById(R.id.selected_notes_label)).setText(label);
    }
}
