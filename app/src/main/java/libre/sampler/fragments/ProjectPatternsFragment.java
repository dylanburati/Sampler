package libre.sampler.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternEvent;
import libre.sampler.utils.PianoRollController;
import libre.sampler.publishers.PatternBuilder;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.PatternThread;

public class ProjectPatternsFragment extends Fragment {
    private PatternEventSource patternEventSource;
    private PatternThread patternThread;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private PianoRollController pianoRollController;

    private Pattern pattern1;
    private PatternBuilder patternBuilder;

    private ImageView patternStop;
    private boolean isRunning;
    private ImageView patternPlay;
    private boolean isPlaying;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        patternEventSource = ((ProjectActivity) getActivity()).patternEventSource;
        patternThread = ((ProjectActivity) getActivity()).patternThread;

        pianoRollContainer = (RecyclerView) rootView.findViewById(R.id.piano_roll_container);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(rootView.getContext(), PianoRollAdapter.SPAN_COUNT, RecyclerView.HORIZONTAL, false);
        pianoRollContainer.setLayoutManager(layoutManager);

        List<Pattern> patterns = new ArrayList<>(patternThread.runningPatterns.values());
        pianoRollController = new PianoRollController(patternThread, patterns);
        pianoRollController.keyHeight = getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        pianoRollController.baseBarWidth = getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth);

        // todo move defaults
        pianoRollController.noteLength = new MusicTime(0, 4, 0);
        pianoRollController.snap = new MusicTime(0, 1, 0);
        pianoRollController.velocity = 100;
        pianoRollController.setLoopLength(new MusicTime(1, 0, 0));
        pianoRollController.setTempo(140);

        pianoRollController.setNoteLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length1),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length2));
        pianoRollController.setSnapInput((Spinner) rootView.findViewById(R.id.piano_roll_settings_snaplength0));
        pianoRollController.setPatternLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength1),
                (Button) rootView.findViewById(R.id.submit_patternlength));
        pianoRollController.setTempoInput((EditText) rootView.findViewById(R.id.pattern_tempo));

        pianoRollContainer.setAdapter(pianoRollController.adapter);

        patternStop = (ImageView) rootView.findViewById(R.id.pattern_stop);
        patternStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning) {
                    isRunning = false;
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
                    if(isPlaying) {
                        isPlaying = false;
                    }
                }
                updatePlayPauseControls(v.getContext());
            }
        });

        patternPlay = (ImageView) rootView.findViewById(R.id.pattern_play);
        patternPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlaying = !isPlaying;
                if(isPlaying) {
                    if(!isRunning) {
                        isRunning = true;
                        patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON, pianoRollController.getActivePattern()));
                    } else {
                        patternThread.resumeLoop();
                    }
                } else {
                    if(isRunning) {
                        patternThread.suspendLoop();
                    }
                }
                updatePlayPauseControls(v.getContext());
            }
        });

        isRunning = (patternThread.runningPatterns.size() > 0);
        isPlaying = (isRunning && !patternThread.isSuspended);
        updatePlayPauseControls(getContext());

        return rootView;
    }

    private void updatePlayPauseControls(Context ctx) {
        if(isRunning) {
            if(isPlaying) {
                patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_pause));
                patternPlay.setContentDescription("Pause");
            } else {
                patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_play));
                patternPlay.setContentDescription("Play");
            }
        } else {
            patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_play));
            patternPlay.setContentDescription("Play");
        }

        patternStop.setEnabled(isRunning);
    }
}
