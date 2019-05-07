package libre.sampler.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.models.PatternEvent;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.utils.PatternThread;

public class ProjectPatternsFragment extends Fragment {
    private PatternEventSource patternEventSource;
    private PatternThread patternThread;
    private Switch patternEnable;

    private Button playPause;
    private boolean playPauseState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        patternEventSource = ((ProjectActivity) getActivity()).patternEventSource;
        patternThread = ((ProjectActivity) getActivity()).patternThread;

        patternEnable = (Switch) rootView.findViewById(R.id.pattern_enable);
        patternEnable.setChecked(patternThread.runningPatterns.containsKey("test"));
        patternEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON));
                } else {
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF));
                }
            }
        });

        playPause = (Button) rootView.findViewById(R.id.pattern_play_pause);
        playPauseState = true;
        playPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPauseState = !playPauseState;
                if(playPauseState) {
                    playPause.setText(R.string.pause);
                    playPause.setCompoundDrawablesWithIntrinsicBounds(null, null, v.getContext().getDrawable(R.drawable.ic_pause), null);
                    patternThread.resumeLoop();
                } else {
                    playPause.setText(R.string.play);
                    playPause.setCompoundDrawablesWithIntrinsicBounds(null, null, v.getContext().getDrawable(R.drawable.ic_play), null);
                    patternThread.suspendLoop();
                }
            }
        });

        return rootView;
    }

}
