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
import java.util.List;
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
import libre.sampler.fragments.patternedit.PatternEditCopyMultiple;
import libre.sampler.fragments.patternedit.PatternEditNoteProperties;
import libre.sampler.fragments.patternedit.PatternEditPatternLength;
import libre.sampler.fragments.patternedit.PatternEditSnapLength;
import libre.sampler.listeners.RecyclerViewScrollChangeDispatcher;
import libre.sampler.listeners.ScrollChangeListener;
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
import libre.sampler.utils.NoteId;
import libre.sampler.views.VisualNote;

public class ProjectPatternsFragment extends Fragment {
    private ProjectViewModel viewModel;
    private ProjectActivity projectActivity;
    private PatternDerivedData patternDerivedData;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private final TreeSet<VisualNote> selectedNotes = new TreeSet<>();
    public EmptyEventSource patternEditEventSource = new EmptyEventSource();

    private double tickWidth;
    private float keyHeight;

    private MusicTime inputNoteLength;
    private MusicTime snap;
    private MusicTime inputLoopLength;
    private double inputTempo;
    public int velocity;

    private TextView pianoRollPosition;
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
        projectActivity = ((ProjectActivity) getActivity());

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

        this.keyHeight = getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        this.tickWidth = getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth) / 1.0 / MusicTime.TICKS_PER_BAR;
        this.inputNoteLength = new MusicTime(0, 4, 0);
        this.snap = new MusicTime(0, 1, 0);
        this.velocity = 100;
        this.inputLoopLength = new MusicTime(viewModel.getPianoRollPattern().getLoopLengthTicks());
        this.inputTempo = viewModel.getPianoRollPattern().getTempo();

        registerTempoInput((EditText) rootView.findViewById(R.id.pattern_tempo));

        pianoRollAdapter = new PianoRollAdapter(this);
        pianoRollContainer.setAdapter(pianoRollAdapter);

        attachEventListeners();

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
                if(projectActivity.isPatternThreadRunning()) {
                    viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
                }
                updatePlayPauseControls(v.getContext());
            }
        });

        patternPlay = (ImageView) rootView.findViewById(R.id.pattern_play);
        patternPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(projectActivity.isPatternThreadRunning()) {
                    if(projectActivity.isPatternThreadPaused()) {
                        projectActivity.getPatternThread().resumeLoop();
                    } else {
                        projectActivity.getPatternThread().suspendLoop();
                    }
                } else {
                    viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_ON, viewModel.getPianoRollPattern()));
                }
                updatePlayPauseControls(v.getContext());
            }
        });

        pianoRollPosition = (TextView) rootView.findViewById(R.id.piano_roll_position);

        RecyclerViewScrollChangeDispatcher scrollChangeDispatcher = new RecyclerViewScrollChangeDispatcher(pianoRollContainer);
        scrollChangeDispatcher.setListener(new ScrollChangeListener() {
            @Override
            public void onScrollChange(int x, int y, int oldX, int oldY) {
                updatePianoRollPosition(x);
            }
        });
        updatePianoRollPosition(0);

        isRunning = projectActivity.isPatternThreadRunning();
        isPlaying = (isRunning && !projectActivity.getPatternThread().isSuspended);
        updatePlayPauseControls(getContext());

        setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);

        return rootView;
    }

    @Override
    public void onResume() {
        updatePlayPauseControls(getContext());
        for(Instrument t : patternDerivedData.getInstrumentList()) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
        }
        super.onResume();
    }

    private void attachEventListeners() {
        viewModel.patternEventSource.add("PatternLoader", new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(event.action == PatternEvent.PATTERN_SELECT || event.action == PatternEvent.PATTERN_CREATE_SELECT) {
                    patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
                    for(Instrument t : patternDerivedData.getInstrumentList()) {
                        viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
                    }
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
    }

    public VisualNote onCreatePianoRollNote(int containerIndex, float x, float y) {
        VisualNote visualNote = new VisualNote(containerIndex);
        double tickWidth = getTickWidth();
        double inputTicks = x / tickWidth;
        long snapTicks = snap.getTicks();

        long startTicks = Math.round(inputTicks / 1.0 / snapTicks - 0.3) * snapTicks;

        long maxTicks = viewModel.getPianoRollPattern().getLoopLengthTicks() - startTicks;
        long noteLengthTicks = inputNoteLength.getTicks();
        if(noteLengthTicks > maxTicks) {
            noteLengthTicks = (long) Math.floor(maxTicks / 1.0 / snapTicks) * snapTicks;
        }

        int keyIndex = (int) (y / keyHeight);

        int keyNum = (9 - containerIndex) * 12 + (11 - keyIndex);
        long baseId = NoteId.createForScheduledNoteEvent(System.currentTimeMillis(), 0);
        visualNote.eventOn = new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId);
        visualNote.eventOff = new ScheduledNoteEvent(startTicks + noteLengthTicks, NoteEvent.NOTE_OFF,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId);

        visualNote.calculateParams();
        addToPianoRollPattern(visualNote);
        return visualNote;
    }

    public void onRemovePianoRollNote(VisualNote visualNote) {
        if(visualNote.eventOn != null && visualNote.eventOff != null) {
            removeFromPianoRollPattern(visualNote);
            if(selectedNotes.remove(visualNote)) {
                patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            }
        }
    }

    private void addToPianoRollPattern(VisualNote visualNote) {
        projectActivity.getPatternLoader().addToPattern(viewModel.getPianoRollPattern(),
                visualNote.eventOn, visualNote.eventOff);
    }

    private void removeFromPianoRollPattern(VisualNote visualNote) {
        projectActivity.getPatternLoader().removeFromPattern(viewModel.getPianoRollPattern(),
                visualNote.eventOn, visualNote.eventOff);
    }

    public boolean toggleSelect(VisualNote n) {
        if(selectedNotes.remove(n)) {
            patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            return false;
        }
        selectedNotes.add(n);
        setNoteLength(n.lengthTicks, false);
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

    public MusicTime getInputNoteLength() {
        return inputNoteLength;
    }

    public void setNoteLength(MusicTime noteLength, boolean fromUser) {
        setNoteLength(noteLength.getTicks(), fromUser);
    }

    public void setNoteLength(long ticks, boolean fromUser) {
        this.inputNoteLength.setTicks(ticks);
        if(fromUser && selectedNotes.size() > 0) {
            long lengthenTicks = inputNoteLength.getTicks() - selectedNotes.first().lengthTicks;

            for(VisualNote n : selectedNotes) {
                removeFromPianoRollPattern(n);
                n.lengthTicks += lengthenTicks;
                n.eventOff.offsetTicks = n.startTicks + n.lengthTicks;

                final VisualNote noteRef = n;
                final int modCount = ++n.modificationCount;
                pianoRollContainer.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(noteRef.modificationCount == modCount) {
                            addToPianoRollPattern(noteRef);
                            pianoRollAdapter.updateNote(noteRef);
                        }
                    }
                }, 50);
            }
        }
    }

    public TreeSet<VisualNote> getSelectedNotes() {
        return selectedNotes;
    }

    public void setNoteStart(MusicTime noteStart) {
        if(selectedNotes.size() == 0) {
            return;
        }
        long moveTicks = noteStart.getTicks() - selectedNotes.first().startTicks;

        for(VisualNote n : selectedNotes) {
            removeFromPianoRollPattern(n);
            n.startTicks += moveTicks;
            n.eventOn.offsetTicks = n.startTicks;
            n.eventOff.offsetTicks = n.startTicks + n.lengthTicks;

            final VisualNote noteRef = n;
            final int modCount = ++n.modificationCount;
            pianoRollContainer.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(noteRef.modificationCount == modCount) {
                        projectActivity.getPatternLoader().addToPattern(viewModel.getPianoRollPattern(), noteRef.eventOn, noteRef.eventOff);
                        pianoRollAdapter.updateNote(noteRef);
                    }
                }
            }, 50);
        }
    }

    public void setPatternLength(MusicTime inputLoopLength) {
        projectActivity.getPatternLoader().setLoopLength(viewModel.getPianoRollPattern(), inputLoopLength);
        pianoRollAdapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
    }

    public void insertMultiplePianoRollNotes(VisualNote src, MusicTime interval, int count) {
        long intervalTicks = interval.getTicks();
        long baseId = src.eventOn.noteId;
        for(int i = 1; i <= count; i++) {
            VisualNote note = new VisualNote(src.containerIndex);
            long noteStartTicks = src.startTicks + intervalTicks * i;
            baseId = NoteId.createDuplicate(baseId);

            note.eventOn = new ScheduledNoteEvent(noteStartTicks,
                    NoteEvent.NOTE_ON, src.eventOn.instrument, src.eventOn.keyNum, src.eventOn.velocity,
                    baseId);

            note.eventOff = new ScheduledNoteEvent(noteStartTicks + src.lengthTicks,
                    NoteEvent.NOTE_OFF, src.eventOn.instrument, src.eventOn.keyNum, 0,
                    baseId);

            note.calculateParams();
            projectActivity.getPatternLoader().addToPattern(viewModel.getPianoRollPattern(), note.eventOn, note.eventOff);
            pianoRollAdapter.updateNote(note);
        }
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
                    projectActivity.getPatternLoader().setTempo(viewModel.getPianoRollPattern(), inputTempo);
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
        if(projectActivity.isPatternThreadRunning()) {
            if(projectActivity.isPatternThreadPaused()) {
                patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_play));
                patternPlay.setContentDescription("Play");
            } else {
                patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_pause));
                patternPlay.setContentDescription("Pause");
            }

            patternStop.setEnabled(true);
        } else {
            patternPlay.setImageDrawable(ctx.getDrawable(R.drawable.ic_play));
            patternPlay.setContentDescription("Play");

            patternStop.setEnabled(false);
        }
    }

    private void updatePianoRollPosition(int scrollX) {
        double beatWidth = tickWidth * MusicTime.TICKS_PER_BEAT;
        double dividerWidth = 0.02 * beatWidth;
        int beats = (int) Math.floor((scrollX - dividerWidth) / beatWidth) + 1;
        int bars = beats / MusicTime.BEATS_PER_BAR;
        int sixteenths = (beats % MusicTime.BEATS_PER_BAR) * MusicTime.SIXTEENTHS_PER_BEAT;
        pianoRollPosition.setText(getResources().getString(R.string.piano_roll_position, bars, sixteenths));
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
            } else if(which == AppConstants.PATTERN_EDITOR_COPY_MULTIPLE) {
                fragment = new PatternEditCopyMultiple();
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
