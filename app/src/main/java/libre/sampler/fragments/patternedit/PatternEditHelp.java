package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import libre.sampler.R;

public class PatternEditHelp extends Fragment {
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_help, container, false);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView tv = rootView.findViewById(R.id.pattern_edit_help_text);
        tv.setText(Html.fromHtml(getString(R.string.pattern_edit_help)));
    }

    @Override
    public void onDestroyView() {
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        super.onDestroyView();
        rootView = null;
    }
}
