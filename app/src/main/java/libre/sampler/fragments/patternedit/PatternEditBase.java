package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;

public class PatternEditBase extends Fragment {
    public static final String TAG = "PatternEditBase";

    private ProjectPatternsFragment patternsFragment;
    private ProjectViewModel viewModel;
    private Spinner instrumentSpinner;
    private ArrayAdapter<String> instrumentSpinnerAdapter;
    private List<Instrument> instrumentSpinnerItems;
    private MaterialButtonToggleGroup noteSelectControls;
    private int noteSelectControlActiveCallCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        View rootView = inflater.inflate(R.layout.fragment_pattern_edit_base, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        instrumentSpinner = rootView.findViewById(R.id.instrument_spinner);
        instrumentSpinnerAdapter = new ArrayAdapter<>(
                instrumentSpinner.getContext(), android.R.layout.simple_spinner_item);
        instrumentSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        instrumentSpinner.setAdapter(instrumentSpinnerAdapter);

        instrumentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.setPianoRollInstrument(instrumentSpinnerItems.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        viewModel.instrumentEventSource.add(TAG, new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_DELETE ||
                        event.action == InstrumentEvent.INSTRUMENT_CREATE ||
                        event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                    instrumentSpinnerItems = viewModel.getProject().getInstruments();
                    updateInstrumentSpinner();
                }
            }
        });
        instrumentSpinnerItems = viewModel.getProject().getInstruments();
        updateInstrumentSpinner();

        noteSelectControls = (MaterialButtonToggleGroup) rootView.findViewById(R.id.note_select_controls);
        noteSelectControls.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                noteSelectControlActiveCallCount++;
                try {
                    if(!isChecked || checkedId == View.NO_ID) {
                        return;
                    }
                    switch(checkedId) {
                        case R.id.select_all:
                            patternsFragment.selectAllNotes();
                            break;
                        case R.id.select_none:
                            patternsFragment.clearSelectedNotes();
                            break;
                        case R.id.select_special:
                            patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_SELECT_SPECIAL);
                            break;
                        default:
                            break;
                    }
                } finally {
                    noteSelectControlActiveCallCount--;
                }
            }
        });

        patternsFragment.patternEditEventSource.add(TAG, new Consumer<String>() {
            @Override
            public void accept(String eventName) {
                if(noteSelectControlActiveCallCount > 0) {
                    return;
                }
                if(eventName.equals(AppConstants.SELECTED_NOTES) || eventName.equals(AppConstants.PIANO_ROLL_NOTES)) {
                    highlightNoteSelectControls();
                }
            }
        });
        highlightNoteSelectControls();

        // rootView.findViewById(R.id.delete_selected_notes).setOnClickListener(new View.OnClickListener() {
        //     @Override
        //     public void onClick(View v) {
        //         patternsFragment.deleteAllSelectedNotes();
        //     }
        // });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        instrumentSpinner.setAdapter(null);
        super.onDestroyView();
        this.instrumentSpinner = null;
        this.noteSelectControls = null;
        viewModel.instrumentEventSource.remove(TAG);
        patternsFragment.patternEditEventSource.remove(TAG);
    }

    private void highlightNoteSelectControls() {
        if(patternsFragment.getSelectedNotes().isEmpty()) {
            noteSelectControls.check(R.id.select_none);
        } else if(patternsFragment.isSelectedNotesComplete()) {
            noteSelectControls.check(R.id.select_all);
        } else {
            noteSelectControls.clearChecked();
        }
    }

    private void updateInstrumentSpinner() {
        if(instrumentSpinner != null && instrumentSpinnerAdapter != null) {
            String[] options = new String[instrumentSpinnerItems.size()];
            for(int i = 0; i < options.length; i++) {
                options[i] = instrumentSpinnerItems.get(i).name;
            }

            instrumentSpinnerAdapter.clear();
            instrumentSpinnerAdapter.addAll(options);
            instrumentSpinner.setSelection(instrumentSpinnerItems.indexOf(viewModel.getPianoRollInstrument()));
        }
    }
}
