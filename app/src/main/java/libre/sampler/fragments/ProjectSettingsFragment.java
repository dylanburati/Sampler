package libre.sampler.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.models.Project;
import libre.sampler.utils.AppConstants;

public class ProjectSettingsFragment extends Fragment {
    private Project project;
    private EditText defaultSamplePathInputView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_settings, container, false);
        project = ((ProjectActivity) getActivity()).project;

        defaultSamplePathInputView = (EditText) rootView.findViewById(R.id.pref_default_sample_path);
        defaultSamplePathInputView.setText(project.getDefaultSamplePath());
        defaultSamplePathInputView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                project.setDefaultSamplePath(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return rootView;
    }

}
