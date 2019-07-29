package libre.sampler.fragments.patternedit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MidiConstants;
import libre.sampler.utils.MusicTime;

public class PatternEditSelectSpecial extends Fragment {
    private static final int MAX_INPUT_BARS = 999;
    private ProjectPatternsFragment patternsFragment;
    private MyListener tapListener;
    private ProjectViewModel viewModel;

    private View rootView;
    private MusicTime inputLeft;
    private MusicTime inputRight;
    private int inputTop;
    private int inputBottom;

    private boolean topLeftLocked;
    private boolean bottomRightLocked;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        patternsFragment = (ProjectPatternsFragment) getParentFragment();
        rootView = inflater.inflate(R.layout.fragment_pattern_edit_select_special, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        topLeftLocked = false;
        bottomRightLocked = false;
        ProjectPatternsFragment.MusicRect visible = patternsFragment.getVisibleRect();
        inputLeft = visible.left;
        inputRight = visible.right;
        inputTop = visible.top;
        inputBottom = visible.bottom;

        updateControl();
        tapListener = new MyListener();
        patternsFragment.addPianoRollTapListener(tapListener);

        rootView.findViewById(R.id.select_special_lock1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                topLeftLocked = !topLeftLocked;
                v.setActivated(topLeftLocked);
            }
        });

        rootView.findViewById(R.id.select_special_lock2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomRightLocked = !bottomRightLocked;
                v.setActivated(bottomRightLocked);
            }
        });

        rootView.findViewById(R.id.select_special_expand_y).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputBottom = AppConstants.PIANO_ROLL_BOTTOM_KEYNUM;
                inputTop = AppConstants.PIANO_ROLL_TOP_KEYNUM;
                updateControl();
            }
        });

        rootView.findViewById(R.id.select_special_expand_x).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputLeft.setTicks(0L);
                inputRight.setTicks(viewModel.getPianoRollPattern().getLoopLengthTicks());
                updateControl();
            }
        });

        rootView.findViewById(R.id.submit_select_special).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(inputLeft.getTicks() > inputRight.getTicks()) {
                    MusicTime swp = inputLeft;
                    inputLeft = inputRight;
                    inputRight = swp;
                }
                if(inputBottom > inputTop) {
                    int swp = inputBottom;
                    inputBottom = inputTop;
                    inputTop = swp;
                }
                patternsFragment.selectNotesInRect(inputTop, inputLeft, inputBottom, inputRight);
            }
        });

        rootView.findViewById(R.id.close_select_special).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternsFragment.setEditorFragment(AppConstants.PATTERN_EDITOR_BACK);
            }
        });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        setEnterTransition(null);
        setExitTransition(null);
        setReenterTransition(null);
        super.onDestroyView();
        this.rootView = null;
        patternsFragment.removePianoRollTapListener(tapListener);
    }

    private void updateControl() {
        String dispTop = getResources().getString(R.string.select_special_top, MidiConstants.getNoteName(inputTop), inputTop);
        String dispLeft = getResources().getString(R.string.select_special_left, inputLeft.toString());
        String dispBottom = getResources().getString(R.string.select_special_bottom, MidiConstants.getNoteName(inputBottom), inputBottom);
        String dispRight = getResources().getString(R.string.select_special_right, inputRight.toString());

        ((TextView) rootView.findViewById(R.id.select_special_top)).setText(dispTop);
        ((TextView) rootView.findViewById(R.id.select_special_left)).setText(dispLeft);
        ((TextView) rootView.findViewById(R.id.select_special_bottom)).setText(dispBottom);
        ((TextView) rootView.findViewById(R.id.select_special_right)).setText(dispRight);
    }

    private class MyListener implements ProjectPatternsFragment.PianoRollTapListener {
        public void onSingleTap(MusicTime xTime, int keyNum) {
            if(!topLeftLocked) {
                inputTop = keyNum;
                inputLeft.set(xTime);
            }
            if(!bottomRightLocked) {
                inputBottom = keyNum;
                inputRight.set(xTime);
            }
            updateControl();
        }
    }
}
