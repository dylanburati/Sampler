package libre.sampler.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.fragments.patternedit.PatternEditBase;
import libre.sampler.fragments.patternedit.PatternEditNoteProperties;
import libre.sampler.fragments.patternedit.PatternEditPatternLength;
import libre.sampler.fragments.patternedit.PatternEditSnapLength;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.PatternDerivedData;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.EmptyEventSource;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.NoTransitionPropagation;
import libre.sampler.utils.PatternLoader;
import libre.sampler.utils.PatternThread;
import libre.sampler.views.VisualNote;

public class ProjectPatternsFragment extends Fragment {
    private ProjectViewModel viewModel;
    private PatternThread patternThread;
    private PatternDerivedData patternDerivedData;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private final Set<VisualNote> selectedNotes = new TreeSet<>();
    private PatternLoader patternLoader;
    public EmptyEventSource patternEditEventSource = new EmptyEventSource();

    private double tickWidth;
    private float keyHeight;

    private MusicTime noteLength;
    private MusicTime snap;
    private MusicTime inputLoopLength;
    private double inputTempo;
    public int velocity;

    private EditText tempoEditText;

    private ImageView patternStop;
    private boolean isRunning;
    private ImageView patternPlay;
    private boolean isPlaying;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);
        patternThread = ((ProjectActivity) getActivity()).patternThread;

        // if landscape and not tablet, put piano roll on left instead of top
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE &&
                getResources().getDisplayMetrics().widthPixels < getResources().getDimensionPixelOffset(R.dimen.split_screen_direction_threshold)) {
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

        patternLoader = new PatternLoader(patternThread);
        this.keyHeight = getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        this.tickWidth = getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth) / 1.0 / MusicTime.TICKS_PER_BAR;
        this.noteLength = new MusicTime(0, 4, 0);
        this.snap = new MusicTime(0, 1, 0);
        this.velocity = 100;
        this.inputLoopLength = new MusicTime(viewModel.getPianoRollPattern().getLoopLengthTicks());
        this.inputTempo = viewModel.getPianoRollPattern().getTempo();

        registerTempoInput((EditText) rootView.findViewById(R.id.pattern_tempo));

        pianoRollAdapter = new PianoRollAdapter(this);
        pianoRollContainer.setAdapter(pianoRollAdapter);

        viewModel.patternEventSource.add("PatternLoader", new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(event.action == PatternEvent.PATTERN_SELECT || event.action == PatternEvent.PATTERN_CREATE_SELECT) {
                    patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
                    for(Instrument t : patternDerivedData.getInstrumentList()) {
                        viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
                    }
                    // updatePatternLengthInputs();
                    updateTempoInput();
                }
            }
        });

        viewModel.instrumentEventSource.add("PatternLoader", new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_PIANO_ROLL_SELECT) {
                    selectedNotes.clear();
                    updatePianoRollNotes();
                    patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
                }
            }
        });

        patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
        for(Instrument t : patternDerivedData.getInstrumentList()) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
        }
        if(!patternDerivedData.getInstrumentList().isEmpty()) {
            viewModel.setPianoRollInstrument(patternDerivedData.getInstrumentList().get(0));
        } else {
            viewModel.setPianoRollInstrument(viewModel.getKeyboardInstrument());
        }
        pianoRollAdapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
        updatePianoRollNotes();

        patternStop = (ImageView) rootView.findViewById(R.id.pattern_stop);
        patternStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning) {
                    isRunning = false;
                    viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
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
                        viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON, viewModel.getPianoRollPattern()));
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

        setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);

        return rootView;
    }

    public void onCreatePianoRollNote(VisualNote outView, int containerIndex, float x, float y) {
        double tickWidth = getTickWidth();
        double inputTicks = x / tickWidth;
        long snapTicks = snap.getTicks();

        long startTicks = Math.round(inputTicks / 1.0 / snapTicks - 0.3) * snapTicks;

        long maxTicks = viewModel.getPianoRollPattern().getLoopLengthTicks() - startTicks;
        long noteLengthTicks = noteLength.getTicks();
        if(noteLengthTicks > maxTicks) {
            noteLengthTicks = (long) Math.floor(maxTicks / 1.0 / snapTicks) * snapTicks;
        }

        int keyIndex = (int) (y / keyHeight);

        int keyNum = (9 - containerIndex) * 12 + (11 - keyIndex);
        long baseId = AppConstants.PATTERN_EVENT_ID_OFFSET + System.currentTimeMillis();
        outView.eventOn = new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId);
        outView.eventOff = new ScheduledNoteEvent(startTicks + noteLengthTicks, NoteEvent.NOTE_OFF,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId);

        outView.calculateParams();
        patternLoader.addToPattern(viewModel.getPianoRollPattern(), outView.eventOn, outView.eventOff);
    }

    public void onRemovePianoRollNote(VisualNote outView) {
        if(outView.eventOn != null && outView.eventOff != null) {
            patternLoader.removeFromPattern(viewModel.getPianoRollPattern(), outView.eventOn, outView.eventOff);
            if(selectedNotes.remove(outView)) {
                patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            }
        }
    }

    public boolean toggleSelect(VisualNote n) {
        if(selectedNotes.remove(n)) {
            patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            return false;
        }
        selectedNotes.add(n);
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
        return true;
    }

    private void updatePianoRollNotes() {
        Instrument pianoRollInstrument = viewModel.getPianoRollInstrument();
        if(pianoRollInstrument != null) {
            List<VisualNote> noteViews = patternDerivedData.getNotesForInstrument(pianoRollInstrument);
            pianoRollAdapter.setPianoRollNotes(noteViews);
        }
    }

    public double getTickWidth() {
        return tickWidth;
    }

    public float getKeyHeight() {
        return keyHeight;
    }

    public MusicTime getSnapLength() {
        return snap;
    }

    public void setSnapLength(MusicTime snapLength) {
        this.snap = snapLength;
    }

    public MusicTime getNoteLength() {
        return noteLength;
    }

    public void setNoteLength(MusicTime noteLength, boolean fromUser) {
        this.noteLength = noteLength;
    }

    public Set<VisualNote> getSelectedNotes() {
        return selectedNotes;
    }

    public void setNoteStart(MusicTime noteStart) {
        if(selectedNotes.size() == 0) {
            return;
        }
        Iterator<VisualNote> iter = selectedNotes.iterator();
        VisualNote n = iter.next();
        long moveTicks = noteStart.getTicks() - n.startTicks;

        boolean iterDone = false;
        while(!iterDone) {
            patternLoader.removeFromPattern(viewModel.getPianoRollPattern(), n.eventOn, n.eventOff);
            n.startTicks += moveTicks;
            n.eventOn.offsetTicks = n.startTicks;
            n.eventOff.offsetTicks = n.startTicks + n.lengthTicks;

            final VisualNote noteRef = n;
            final int modCount = ++n.modificationCount;
            pianoRollContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(noteRef.modificationCount == modCount) {
                        patternLoader.addToPattern(viewModel.getPianoRollPattern(), noteRef.eventOn, noteRef.eventOff);
                        pianoRollAdapter.updateNote(noteRef);
                    }
                }
            }, 50);

            iterDone = !iter.hasNext();
            if(!iterDone) {
                n = iter.next();
            }
        }
    }

    public void setPatternLength(MusicTime inputLoopLength) {
        patternLoader.setLoopLength(viewModel.getPianoRollPattern(), inputLoopLength);
        pianoRollAdapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
    }

    private void registerTempoInput(EditText tempoEditText) {
        this.tempoEditText = tempoEditText;

        tempoEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    inputTempo = Double.parseDouble(s.toString());
                } catch(NumberFormatException ignored) {
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        tempoEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE) {
                    patternLoader.setTempo(viewModel.getPianoRollPattern(), inputTempo);
                }
                return false;
            }
        });

        updateTempoInput();
    }

    private void updateTempoInput() {
        if(tempoEditText != null) {
            if(viewModel.getPianoRollPattern() != null) {
                inputTempo = viewModel.getPianoRollPattern().getTempo();
            }
            DecimalFormat fmt = new DecimalFormat("0.###");
            tempoEditText.setText(fmt.format(inputTempo));
        }
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

    public void setEditorFragment(int which) {
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        Fragment fragment = null;

        if(which == AppConstants.PATTERN_EDITOR_BASE) {
            if(fm.getBackStackEntryCount() == 0) {
                fragment = new PatternEditBase();
                Transition trIn = new Slide(Gravity.LEFT).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                trIn.setPropagation(new NoTransitionPropagation());
                fragment.setEnterTransition(trIn);
                Transition trOut = new Slide(Gravity.LEFT).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
                trOut.setPropagation(new NoTransitionPropagation());
                fragment.setExitTransition(trOut);
            } else {
                fm.popBackStack();
            }
        } else {
            transaction.addToBackStack(null);
            if(which == AppConstants.PATTERN_EDITOR_SNAP_LENGTH) {
                fragment = new PatternEditSnapLength();
            } else if(which == AppConstants.PATTERN_EDITOR_NOTE_PROPERTIES) {
                fragment = new PatternEditNoteProperties();
            } else if(which == AppConstants.PATTERN_EDITOR_PATTERN_LENGTH) {
                fragment = new PatternEditPatternLength();
            } else {
                return;
            }

            Transition trIn = new Slide(Gravity.RIGHT).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            trIn.setPropagation(new NoTransitionPropagation());
            fragment.setEnterTransition(trIn);
            Transition trOut = new Slide(Gravity.RIGHT).setDuration(getResources().getInteger(android.R.integer.config_shortAnimTime));
            trOut.setPropagation(new NoTransitionPropagation());
            fragment.setExitTransition(trOut);
        }

        if(fragment != null) {
            transaction.replace(R.id.pattern_edit_fragment_container, fragment);
            transaction.commit();
        }
    }
}
