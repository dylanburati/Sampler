package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

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
    private ProjectPatternsFragment patternsFragment;
    private ProjectViewModel viewModel;
    private Spinner instrumentSpinner;
    private ArrayAdapter<String> instrumentSpinnerAdapter;
    private List<Instrument> instrumentSpinnerItems;

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

        viewModel.instrumentEventSource.add("PatternEditBase", new Consumer<InstrumentEvent>() {
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

        final int[] buttonFragmentPairs = new int[]{R.id.open_snap_length, AppConstants.PATTERN_EDITOR_SNAP_LENGTH,
                R.id.open_note_properties, AppConstants.PATTERN_EDITOR_NOTE_PROPERTIES,
                R.id.open_pattern_length, AppConstants.PATTERN_EDITOR_PATTERN_LENGTH};

        for(int i = 0; i < buttonFragmentPairs.length; i += 2) {
            final int fragmentConst = buttonFragmentPairs[i + 1];
            rootView.findViewById(buttonFragmentPairs[i]).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    patternsFragment.setEditorFragment(fragmentConst);
                }
            });
        }

        return rootView;
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
