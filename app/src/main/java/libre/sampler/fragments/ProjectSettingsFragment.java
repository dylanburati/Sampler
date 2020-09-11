package libre.sampler.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.TouchVelocitySource;

public class ProjectSettingsFragment extends Fragment {
    private ProjectViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_settings, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        EditText defaultSamplePathInputView = rootView.findViewById(R.id.pref_default_sample_path);
        defaultSamplePathInputView.setText(viewModel.getProject().getDefaultSamplePath());
        defaultSamplePathInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.getProject().setDefaultSamplePath(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        EditText defaultExportPathInputView = rootView.findViewById(R.id.pref_default_instrument_export_path);
        defaultExportPathInputView.setText(viewModel.getProject().getDefaultExportPath());
        defaultExportPathInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                viewModel.getProject().setDefaultExportPath(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        final Map<Integer, TouchVelocitySource> viewIdToSource = new HashMap<>();
        viewIdToSource.put(R.id.radio_button_touch_velocity_none, TouchVelocitySource.NONE);
        viewIdToSource.put(R.id.radio_button_touch_velocity_location, TouchVelocitySource.LOCATION);
        viewIdToSource.put(R.id.radio_button_touch_velocity_pressure, TouchVelocitySource.PRESSURE);
        final Map<TouchVelocitySource, Integer> sourceToViewId = new HashMap<>();
        for (Map.Entry<Integer, TouchVelocitySource> e : viewIdToSource.entrySet()) {
            sourceToViewId.put(e.getValue(), e.getKey());
        }

        RadioGroup touchVelocityGroup = rootView.findViewById(R.id.pref_touch_velocity);
        Integer initialViewId = sourceToViewId.get(viewModel.getProject().getTouchVelocitySource());
        if (initialViewId != null) touchVelocityGroup.check(initialViewId);
        touchVelocityGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                TouchVelocitySource nextSrc = viewIdToSource.get(radioGroup.getCheckedRadioButtonId());
                if (nextSrc != null) viewModel.getProject().setTouchVelocitySource(nextSrc);
            }
        });

        return rootView;
    }

}
