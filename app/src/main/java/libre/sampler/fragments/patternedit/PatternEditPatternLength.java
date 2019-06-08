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
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.views.MusicTimePicker;

public class PatternEditPatternLength extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private View rootView;
    private MusicTimePicker loopLengthPicker;
    private ProjectViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_pattern_length, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        initPicker();

        rootView.findViewById(R.id.submit_pattern_length).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setPatternLength(loopLengthPicker.getValue());
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

    private void initPicker() {
        loopLengthPicker = new MusicTimePicker((NumberPicker) rootView.findViewById(R.id.pattern_length_bars),
                (NumberPicker) rootView.findViewById(R.id.pattern_length_sixteenths)) {
            @Override
            public void onValueChanged(MusicTime value) {
            }
        };
        loopLengthPicker.setTicks(viewModel.getPianoRollPattern().getLoopLengthTicks());
    }
}
