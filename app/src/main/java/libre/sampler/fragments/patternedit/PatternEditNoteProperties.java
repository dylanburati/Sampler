package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.listeners.StatefulTextWatcher;
import libre.sampler.listeners.StatefulVerticalSliderChangeListener;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.MusicTimePicker;
import libre.sampler.views.VerticalSlider;
import libre.sampler.views.VisualNote;

public class PatternEditNoteProperties extends Fragment {
    public static final String TAG = "PatternEditNoteProperties";
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
        initNoteVelocitySlider();

        patternsFragment.patternEditEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(eventName.equals(AppConstants.SELECTED_NOTES)) {
                    updateSelectedLabel();
                    updateNoteStartPicker();
                    updateNoteLengthPicker();
                    updateNoteVelocitySlider();
                }
            }
        });
        updateSelectedLabel();

        return rootView;
    }

    private void initNoteStartPicker() {
        noteStartPicker = rootView.findViewById(R.id.note_start_picker);
        noteStartPicker.setOnValueChangedListener(new MusicTimePicker.OnValueChangedListener() {
            @Override
            public void onValueChange(MusicTime value) {
                if(selectedNotes.size() > 0) {
                    patternsFragment.setNoteStart(value);
                }
            }
        });

        updateNoteStartPicker();
    }

    @Override
    public void onDestroyView() {
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        super.onDestroyView();
        this.rootView = null;
        this.noteStartPicker = null;
        this.noteLengthPicker = null;
        patternsFragment.patternEditEventSource.remove(TAG);
    }

    private void initNoteLengthPicker() {
        noteLengthPicker = rootView.findViewById(R.id.note_length_picker);
        noteLengthPicker.setOnValueChangedListener(new MusicTimePicker.OnValueChangedListener() {
            @Override
            public void onValueChange(MusicTime value) {
                patternsFragment.setNoteLength(value, true);
            }
        });

        updateNoteLengthPicker();
    }

    private boolean preventDispatch = false;
    private void initNoteVelocitySlider() {
        VerticalSlider noteVelocitySlider = rootView.findViewById(R.id.note_velocity_slider);
        EditText noteVelocityInput = rootView.findViewById(R.id.note_velocity);
        noteVelocityInput.addTextChangedListener(new StatefulTextWatcher<VerticalSlider>(noteVelocitySlider) {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int val = 0;
                try {
                    val = Math.round(Float.parseFloat(s.toString()));
                } catch(NumberFormatException ignored) {
                }
                this.data.setProgress(val / 127f);
                if(!preventDispatch) {
                    patternsFragment.setNoteVelocity(val);
                }
            }
        });
        noteVelocitySlider.addListener(new StatefulVerticalSliderChangeListener<EditText>(noteVelocityInput) {
            @Override
            public void onProgressChanged(VerticalSlider v, float progress, boolean fromUser) {
                if(fromUser) {
                    int val = Math.round(progress * 127);
                    this.data.setText(Integer.toString(val));
                    patternsFragment.setNoteVelocity(val);
                }
            }
        });

        updateNoteVelocitySlider();
    }

    private final MusicTime tmpNoteStart = new MusicTime(0L);
    private void updateNoteStartPicker() {
        int visibility = (selectedNotes.size() > 0) ? View.VISIBLE : View.GONE;
        noteStartPicker.setVisibility(visibility);
        rootView.findViewById(R.id.note_start_label).setVisibility(visibility);
        rootView.findViewById(R.id.note_velocity_container).setVisibility(visibility);

        if(selectedNotes.size() > 0) {
            tmpNoteStart.setTicks(selectedNotes.first().startTicks);
            noteStartPicker.setValue(tmpNoteStart);
        }
    }

    private void updateNoteLengthPicker() {
        MusicTime inputNoteLength = patternsFragment.getInputNoteLength();
        noteLengthPicker.setValue(inputNoteLength);
    }

    private void updateNoteVelocitySlider() {
        this.preventDispatch = true;
        if(selectedNotes.size() == 1) {
            int velocity = selectedNotes.first().eventOn.velocity;
            ((EditText) rootView.findViewById(R.id.note_velocity)).setText(Integer.toString(velocity));
        }
        this.preventDispatch = false;
    }

    private void updateSelectedLabel() {
        int num = selectedNotes.size();
        String label = getResources().getQuantityString(R.plurals.selected_notes_label, num, num);
        ((TextView) rootView.findViewById(R.id.selected_notes_label)).setText(label);
    }
}
