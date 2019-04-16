package libre.sampler.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import libre.sampler.R;
import libre.sampler.utils.AppConstants;

public class ProjectSettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_settings, container, false);

        final SharedPreferences sharedPreferences = rootView.getContext().getSharedPreferences(
                AppConstants.TAG_SHARED_PREFS, Context.MODE_PRIVATE);

        ((EditText) rootView.findViewById(R.id.pref_default_sample_path)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(AppConstants.PREF_DEFAULT_SAMPLE_PATH, s.toString());
                editor.apply();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return rootView;
    }

}
