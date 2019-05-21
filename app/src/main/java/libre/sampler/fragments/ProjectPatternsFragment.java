package libre.sampler.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.models.PatternEvent;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.utils.PatternThread;
import libre.sampler.utils.PianoRollController;

public class ProjectPatternsFragment extends Fragment {
    private ProjectActivity projectActivity;
    private PatternThread patternThread;
    private PatternEventSource patternEventSource;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private PianoRollController pianoRollController;

    private ImageView patternStop;
    private boolean isRunning;
    private ImageView patternPlay;
    private boolean isPlaying;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        projectActivity = (ProjectActivity) getActivity();
        patternThread = projectActivity.patternThread;
        patternEventSource = projectActivity.patternEventSource;

        // if landscape and not tablet, put piano roll on left instead of top
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                getResources().getDisplayMetrics().widthPixels < getResources().getDimensionPixelOffset(R.dimen.piano_roll_direction_threshold)) {
            LinearLayout patternsBody = rootView.findViewById(R.id.patterns_body);
            patternsBody.setOrientation(LinearLayout.HORIZONTAL);
            int nChildren = patternsBody.getChildCount();
            for(int i = 0; i < nChildren; i++) {
                ViewGroup.LayoutParams params = patternsBody.getChildAt(i).getLayoutParams();
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                params.width = 0;
                patternsBody.getChildAt(i).setLayoutParams(params);
            }
        }

        pianoRollContainer = (RecyclerView) rootView.findViewById(R.id.piano_roll_container);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(rootView.getContext(), PianoRollAdapter.SPAN_COUNT, RecyclerView.HORIZONTAL, false);
        pianoRollContainer.setLayoutManager(layoutManager);

        pianoRollController = new PianoRollController(projectActivity);
        pianoRollController.keyHeight = getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        pianoRollController.baseBarWidth = getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth);

        pianoRollController.registerInstrumentInput((Spinner) rootView.findViewById(R.id.piano_roll_settings_instrument));
        pianoRollController.registerNoteLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length1),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length2));
        pianoRollController.registerSnapInput((Spinner) rootView.findViewById(R.id.piano_roll_settings_snaplength0));
        pianoRollController.registerPatternLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength1),
                (Button) rootView.findViewById(R.id.submit_patternlength));
        pianoRollController.registerTempoInput((EditText) rootView.findViewById(R.id.pattern_tempo));

        pianoRollContainer.setAdapter(pianoRollController.adapter);
        pianoRollController.setPianoRollNotes(pianoRollContainer.getContext());

        patternStop = (ImageView) rootView.findViewById(R.id.pattern_stop);
        patternStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning) {
                    isRunning = false;
                    patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
                    if(isPlaying) {
                        isPlaying = false;
                    } else {
                        patternThread.resumeLoop();
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
