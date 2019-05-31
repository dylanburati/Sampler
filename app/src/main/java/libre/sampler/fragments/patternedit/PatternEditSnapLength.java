package libre.sampler.fragments.patternedit;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;

public class PatternEditSnapLength extends Fragment {
    private ProjectPatternsFragment patternsFragment;

    private static final String[] SNAP_OPTION_DESCRIPTIONS = new String[]{
            "Whole note | 1:00:00",
            "Quarter note | 0:04:00",
            "Eighth note | 0:02:00",
            "Triplet | 0:01:08",
            "Sixteenth | 0:01:00",
            "1/2 triplet | 0:00:16",
            "Thirty-second | 0:00:12",
            "1/4 triplet | 0:00:08",
            "64th | 0:00:06",
            "None | 0:00:01"
    };
    private static final MusicTime[] SNAP_OPTIONS = new MusicTime[]{
            new MusicTime(1, 0, 0),
            new MusicTime(0, 4, 0),
            new MusicTime(0, 2, 0),
            new MusicTime(0, 1, 8),
            new MusicTime(0, 1, 0),
            new MusicTime(0, 0, 16),
            new MusicTime(0, 0, 12),
            new MusicTime(0, 0, 8),
            new MusicTime(0, 0, 6),
            new MusicTime(0, 0, 1)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        View rootView = inflater.inflate(R.layout.fragment_pattern_edit_snap, container, false);

        Spinner spinner = rootView.findViewById(R.id.snap_length_spinner);
        SpannableString[] spinnerOptions = new SpannableString[SNAP_OPTION_DESCRIPTIONS.length];
        generateOptions(spinnerOptions, spinner.getContext());
        ArrayAdapter<SpannableString> spinnerAdapter = new ArrayAdapter<>(
                spinner.getContext(), android.R.layout.simple_spinner_item, spinnerOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        int currentSelection = 0;
        long currentTicks = patternsFragment.getSnapLength().getTicks();
        for(/* int currentSelection = 0 */; currentSelection < SNAP_OPTIONS.length - 1; currentSelection++) {
            if(SNAP_OPTIONS[currentSelection].getTicks() <= currentTicks) {
                break;
            }
        }
        spinner.setSelection(currentSelection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                patternsFragment.setSnapLength(SNAP_OPTIONS[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        rootView.findViewById(R.id.close_snap_length).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);
            }
        });

        return rootView;
    }

    private void generateOptions(SpannableString[] outOptions, Context ctx) {
        for(int i = 0; i < SNAP_OPTION_DESCRIPTIONS.length; i++) {
            outOptions[i] = new SpannableString(SNAP_OPTION_DESCRIPTIONS[i]);
            outOptions[i].setSpan(new TextAppearanceSpan(ctx, R.style.TextAppearanceMonospace),
                    SNAP_OPTION_DESCRIPTIONS[i].indexOf("|") + 2, SNAP_OPTION_DESCRIPTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
