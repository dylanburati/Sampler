package libre.sampler.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.PianoRollSettings;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.PatternBuilder;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.PatternThread;

public class ProjectPatternsFragment extends Fragment {
    private PatternEventSource patternEventSource;
    private PatternThread patternThread;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private PianoRollSettings pianoRollSettings;

    private Pattern pattern1;
    private PatternBuilder patternBuilder;

    private Switch patternEnable;

    private Button playPause;
    private boolean playPauseState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        patternEventSource = ((ProjectActivity) getActivity()).patternEventSource;
        patternThread = ((ProjectActivity) getActivity()).patternThread;

        pianoRollContainer = (RecyclerView) rootView.findViewById(R.id.piano_roll_container);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(rootView.getContext(), PianoRollAdapter.SPAN_COUNT, RecyclerView.HORIZONTAL, false);
        pianoRollContainer.setLayoutManager(layoutManager);

        pianoRollSettings = new PianoRollSettings();
        pianoRollSettings.lengthUserTicks = AppConstants.USER_TICKS_PER_BEAT;
        pianoRollSettings.snapUserTicks = AppConstants.USER_TICKS_PER_BEAT / 4;
        pianoRollSettings.velocity = 100;
        pianoRollSettings.containerLengthUserTicks = AppConstants.USER_TICKS_PER_BEAT * AppConstants.BEATS_PER_BAR;

        pattern1 = new Pattern(new ArrayList<ScheduledNoteEvent>());
        pattern1.setLoopLengthTicks(AppConstants.BEATS_PER_BAR * AppConstants.TICKS_PER_BEAT);
        pattern1.setTempo(153);

        patternBuilder = new PatternBuilder(patternThread, pattern1);
        patternBuilder.setOnChangedListener(new Consumer<List<ScheduledNoteEvent>>() {
            @Override
            public void accept(List<ScheduledNoteEvent> events) {
                Log.d("ScheduledNoteEvent", "events changed");
            }
        });
        pianoRollAdapter = new PianoRollAdapter(pianoRollSettings, patternBuilder);
        pianoRollContainer.setAdapter(pianoRollAdapter);

        patternEnable = (Switch) rootView.findViewById(R.id.pattern_enable);
        patternEnable.setChecked(patternThread.runningPatterns.containsKey("test"));
        patternEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON, pattern1));
                } else {
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
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
