package libre.sampler.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProviders;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRoll;
import libre.sampler.fragments.patternedit.PatternEditBase;
import libre.sampler.fragments.patternedit.PatternEditCopyMultiple;
import libre.sampler.fragments.patternedit.PatternEditNoteProperties;
import libre.sampler.fragments.patternedit.PatternEditPatternLength;
import libre.sampler.fragments.patternedit.PatternEditSelectSpecial;
import libre.sampler.fragments.patternedit.PatternEditSnapLength;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternDerivedData;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.EmptyEventSource;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.LabelHelper;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.NoteId;
import libre.sampler.utils.PatternThread;
import libre.sampler.views.IconNavigationPanel;
import libre.sampler.views.VisualNote;

public class ProjectPatternsFragment extends Fragment {
    public static final String TAG = "ProjectPatternsFragment";
    private ProjectViewModel viewModel;
    private ProjectActivity projectActivity;
    private PatternDerivedData patternDerivedData;

    private LinearLayout pianoRollTimeLabels;
    private PianoRoll pianoRoll;
    private final TreeSet<VisualNote> selectedNotes = new TreeSet<>();
    public EmptyEventSource patternEditEventSource = new EmptyEventSource();
    private List<PianoRollTapListener> pianoRollTapListeners = new ArrayList<>();

    private double inputTempo;

    private TextView pianoRollPosition;

    private EditText tempoEditText;
    private ImageView patternStop;
    private ImageView patternPlay;

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

        final float octaveHeight = getResources().getDimension(R.dimen.piano_roll_colheight);
        this.inputTempo = viewModel.getPianoRollPattern().getTempo();

        registerTempoInput((EditText) rootView.findViewById(R.id.pattern_tempo));

        pianoRoll = new PianoRoll(this, (LinearLayout) rootView.findViewById(R.id.piano_roll));
        pianoRollTimeLabels = rootView.findViewById(R.id.piano_roll_time_labels);
        LinearLayout pianoRollOctaveLabels = rootView.findViewById(R.id.piano_roll_octave_labels);
        LabelHelper.refreshSegmented(pianoRollOctaveLabels, 0, 8,
                R.layout.component_piano_roll_octave_label,
                new LabelHelper.LabelSelector() {
                    @Override
                    public void setup(int index, TextView outLabel) {
                        outLabel.getLayoutParams().height = LabelHelper.getLabelSize(octaveHeight, index);
                        outLabel.setText(String.format("C%d", 8 - index));
                    }
                });
        attachEventListeners();

        patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
        for(Instrument t : patternDerivedData.getInstrumentList()) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
        }
        if(!patternDerivedData.getInstrumentList().isEmpty()) {
            viewModel.setPianoRollInstrument(patternDerivedData.getInstrumentList().iterator().next());
        } else {
            viewModel.setPianoRollInstrument(viewModel.getKeyboardInstrument());
        }
        updatePianoRollTicks(viewModel.getPianoRollPattern().getLoopLengthTicks());
        setPianoRollNotes();

        patternStop = (ImageView) rootView.findViewById(R.id.pattern_stop);
        patternStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(projectActivity.isPatternThreadRunning()) {
                    viewModel.patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_OFF, null));
                }
                updatePianoRollPosition();
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
                updatePianoRollPosition();
                updatePlayPauseControls(v.getContext());
            }
        });

        pianoRollPosition = (TextView) rootView.findViewById(R.id.piano_roll_position);
        updatePianoRollPosition();
        updatePlayPauseControls(getContext());

        registerEditorFragmentNav((IconNavigationPanel) rootView.findViewById(R.id.pattern_edit_nav));
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

    @Override
    public void onDestroyView() {
        pianoRoll = null;
        super.onDestroyView();
        this.pianoRollTimeLabels = null;
        this.patternPlay = null;
        this.patternStop = null;
        this.pianoRollPosition = null;
        this.tempoEditText = null;
        viewModel.patternEventSource.remove(TAG);
        viewModel.instrumentEventSource.remove(TAG);
    }

    private void attachEventListeners() {
        viewModel.patternEventSource.add(TAG, new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(event.action == PatternEvent.PATTERN_SELECT || event.action == PatternEvent.PATTERN_CREATE_SELECT) {
                    patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
                    for(Instrument t : patternDerivedData.getInstrumentList()) {
                        viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
                    }
                    updateTempoInput();
                    updatePianoRollPosition();
                }
            }
        });

        viewModel.instrumentEventSource.add(TAG, new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_PIANO_ROLL_SELECT) {
                    selectedNotes.clear();
                    setPianoRollNotes();
                    patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
                }
            }
        });
    }

    /**
     * Called when the notes to be displayed are changed from somewhere other than the
     * PianoRoll.
     *
     * <p>Changes to the properties of notes are handled by {@link PianoRoll#setNoteActivated}
     */
    private void setPianoRollNotes() {
        Instrument pianoRollInstrument = viewModel.getPianoRollInstrument();
        if(pianoRollInstrument != null) {
            List<VisualNote> noteViews = patternDerivedData.getNotesForInstrument(pianoRollInstrument);
            pianoRoll.setPianoRollNotes(noteViews);
        }
    }

    private void updatePianoRollTicks(long ticks) {
        pianoRoll.updateRollTicks(ticks);
        final int px = pianoRoll.getRollWidth();
        final int labelCount = 1 + (int) ((ticks - 1) / MusicTime.TICKS_PER_BAR);
        final double barWidth = pianoRoll.getTickWidth() * MusicTime.TICKS_PER_BAR;
        LabelHelper.refreshSegmented(pianoRollTimeLabels, 1, labelCount,
                R.layout.component_piano_roll_time_label,
                new LabelHelper.LabelSelector() {
                    @Override
                    public void setup(int index, TextView outLabel) {
                        if(index < labelCount - 1) {
                            outLabel.getLayoutParams().width = LabelHelper.getLabelSize(barWidth, index);
                        } else {
                            outLabel.getLayoutParams().width = px - (int) Math.round(barWidth * (labelCount - 1));
                        }
                        outLabel.setText(String.format("%d", index));
                    }
                });
    }

    public void addToPianoRollPattern(VisualNote visualNote) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.addEvent(visualNote.eventOn);
            editor.pattern.addEvent(visualNote.eventOff);
            if(visualNote.eventOn.instrument == viewModel.getPianoRollInstrument()) {
                pianoRoll.addNote(visualNote, selectedNotes.contains(visualNote));
            }
        }

        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
    }

    private void addBatchToPianoRollPattern(Collection<VisualNote> visualNotes) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            for(VisualNote note : visualNotes) {
                editor.pattern.addEvent(note.eventOn);
                editor.pattern.addEvent(note.eventOff);
                if(note.eventOn.instrument == viewModel.getPianoRollInstrument()) {
                    pianoRoll.addNote(note, selectedNotes.contains(note));
                }
            }
        }

        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
    }

    public void removeFromPianoRollPattern(VisualNote visualNote, boolean willAddBack) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.removeEvent(visualNote.eventOn);
            NoteEvent sendOff = editor.pattern.removeAndGetEvent(visualNote.eventOff);
            if(sendOff != null) {
                viewModel.noteEventSource.dispatch(sendOff);
            }
            if(visualNote.eventOn.instrument == viewModel.getPianoRollInstrument()) {
                pianoRoll.removeNote(visualNote, willAddBack);
            }
        }

        if(!willAddBack) {
            patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
            if(selectedNotes.remove(visualNote)) {
                patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            }
        }
    }

    private void removeBatchFromPianoRollPattern(Collection<VisualNote> visualNotes) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            for(VisualNote note : visualNotes) {
                editor.pattern.removeEvent(note.eventOn);
                NoteEvent sendOff = editor.pattern.removeAndGetEvent(note.eventOff);
                if(sendOff != null) {
                    viewModel.noteEventSource.dispatch(sendOff);
                }
                pianoRoll.removeNote(note, false);
            }
        }

        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
    }

    private void removeBatchFromPianoRollPattern(Collection<VisualNote> visualNotes, List<VisualNote> source) {
        List<VisualNote> adapterNotes = pianoRoll.getPianoRollNotes();
        if(source == adapterNotes) {
            removeBatchFromPianoRollPattern(visualNotes);
            return;
        }
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            for(VisualNote note : visualNotes) {
                editor.pattern.removeEvent(note.eventOn);
                NoteEvent sendOff = editor.pattern.removeAndGetEvent(note.eventOff);
                if(sendOff != null) {
                    viewModel.noteEventSource.dispatch(sendOff);
                }
                source.remove(note);
            }
        }

        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public Instrument getPianoRollInstrument() {
        return viewModel.getPianoRollInstrument();
    }

    public TreeSet<VisualNote> getSelectedNotes() {
        return selectedNotes;
    }

    public boolean toggleNoteSelected(VisualNote n) {
        if(selectedNotes.remove(n)) {
            patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            return false;
        }
        selectedNotes.add(n);
        pianoRoll.setInputNoteLength(n.lengthTicks);
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
        return true;
    }

    /* Select notes */
    public void clearSelectedNotes() {
        selectedNotes.clear();
        pianoRoll.deselectAllNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }
    
    public void selectAllNotes() {
        selectedNotes.addAll(pianoRoll.getPianoRollNotes());
        pianoRoll.selectAllNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public boolean isSelectedNotesComplete() {
        return (pianoRoll.getPianoRollNotes().size() == selectedNotes.size());
    }

    public void deleteAllSelectedNotes() {
        removeBatchFromPianoRollPattern(selectedNotes);
        selectedNotes.clear();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public void selectNotesInRect(int top, MusicTime left, int bottom, MusicTime right) {
        selectedNotes.clear();

        long rightTicks = right.getTicks();
        long leftTicks = left.getTicks();
        for(VisualNote note : pianoRoll.getPianoRollNotes()) {
            if(note.eventOn.keyNum <= top && note.eventOn.keyNum >= bottom &&
                    note.startTicks <= rightTicks && (note.startTicks + note.lengthTicks >= leftTicks)) {

                selectedNotes.add(note);
            }
        }
        pianoRoll.syncSelectedNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public PianoRoll.MusicRect getVisibleRect() {
        return pianoRoll.getVisibleRect();
    }

    public void addPianoRollTapListener(PianoRollTapListener listener) {
        pianoRollTapListeners.add(listener);
    }

    public void removePianoRollTapListener(PianoRollTapListener listener) {
        pianoRollTapListeners.remove(listener);
    }

    public void dispatchPianoRollTap(MusicTime xTime, int keyNum) {
        for(PianoRollTapListener fn : pianoRollTapListeners) {
            fn.onSingleTap(xTime, keyNum);
        }
    }

    /* Note properties */
    public void setNoteLength(MusicTime noteLength, boolean fromUser) {
        setNoteLength(noteLength.getTicks(), fromUser);
    }

    public void setNoteLength(long ticks, boolean fromUser) {
        pianoRoll.setInputNoteLength(ticks);
        if(fromUser && selectedNotes.size() > 0) {
            long lengthenTicks = ticks - selectedNotes.first().lengthTicks;

            for(VisualNote n : selectedNotes) {
                removeFromPianoRollPattern(n, true);
                n.lengthTicks += lengthenTicks;
                n.eventOff.offsetTicks = n.startTicks + n.lengthTicks;

                final VisualNote noteRef = n;
                final int modCount = ++n.modificationCount;
                pianoRoll.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(noteRef.modificationCount == modCount) {
                            addToPianoRollPattern(noteRef);
                        }
                    }
                }, 100);
            }
        }
    }

    public void setNoteStart(MusicTime noteStart) {
        if(selectedNotes.size() == 0) {
            return;
        }
        long moveTicks = noteStart.getTicks() - selectedNotes.first().startTicks;

        for(VisualNote n : selectedNotes) {
            removeFromPianoRollPattern(n, true);
            n.startTicks += moveTicks;
            n.eventOn.offsetTicks = n.startTicks;
            n.eventOff.offsetTicks = n.startTicks + n.lengthTicks;

            final VisualNote noteRef = n;
            final int modCount = ++n.modificationCount;
            pianoRoll.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if(noteRef.modificationCount == modCount) {
                        addToPianoRollPattern(noteRef);
                    }
                }
            }, 100);
        }
    }

    public MusicTime getInputNoteLength() {
        return pianoRoll.getInputNoteLength();
    }

    /* Copy multiple */
    public void copyMultiplePianoRollNotes(MusicTime interval, int count) {
        List<VisualNote> toAdd = new ArrayList<>();
        long intervalTicks = interval.getTicks();
        long baseId = NoteId.createForScheduledNoteEvent(System.currentTimeMillis(), 0);
        for(int i = 1; i <= count; i++) {
            for(VisualNote src : selectedNotes) {
                long startTicks = src.startTicks + intervalTicks * i;
                VisualNote visualNote = new VisualNote(
                        new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                                src.eventOn.instrument, src.eventOn.keyNum, src.eventOn.velocity, baseId),
                        new ScheduledNoteEvent(startTicks + src.lengthTicks, NoteEvent.NOTE_OFF,
                                src.eventOff.instrument, src.eventOff.keyNum, src.eventOff.velocity, baseId)
                );

                toAdd.add(visualNote);
                baseId = NoteId.createDuplicate(baseId);
            }
        }
        addBatchToPianoRollPattern(toAdd);
    }

    /* Snap length */
    public MusicTime getSnapLength() {
        return pianoRoll.getSnapLength();
    }

    public void setSnapLength(MusicTime snapLength) {
        pianoRoll.setSnapLength(snapLength);
    }

    /* Pattern length */
    public void setPatternLength(MusicTime inputLoopLength) {
        long prevLength = viewModel.getPianoRollPattern().getLoopLengthTicks();
        if(prevLength > inputLoopLength.getTicks()) {
            deleteNotesAfterTicks(inputLoopLength.getTicks());
        }
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.setLoopLengthTicks(inputLoopLength.getTicks());
            updatePianoRollTicks(inputLoopLength.getTicks());
        }
        updatePianoRollPosition();
    }

    private void deleteNotesAfterTicks(long maxTicks) {
        for(Instrument instrument : patternDerivedData.getInstrumentList()) {
            List<VisualNote> toDelete = new ArrayList<>();
            List<VisualNote> source = patternDerivedData.getNotesForInstrument(instrument);
            // source.descendingIterator()
            for(VisualNote note : source) {
                if(note.startTicks + note.lengthTicks > maxTicks) {
                    toDelete.add(note);
                }
            }
            removeBatchFromPianoRollPattern(toDelete, source);
        }
    }

    /* Bottom bar */
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
                    try(PatternThread.Editor editor = projectActivity.getPatternThread()
                            .getEditor(viewModel.getPianoRollPattern())) {

                        editor.pattern.setTempo(inputTempo);
                    }
                    updatePianoRollPosition();
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

    private int currentPositionTrackerId = 0;
    private void updatePianoRollPosition() {
        final Pattern pianoRollPattern = viewModel.getPianoRollPattern();
        final int newId = ++currentPositionTrackerId;

        if(!pianoRollPattern.isStarted()) {
            pianoRollPosition.setText(R.string.zero_music_time);
        } else if(pianoRollPattern.isPaused()) {
            return;
        }

        Runnable tracker = new Runnable() {
            private final int id = newId;
            private final long loopLengthTicks = pianoRollPattern.getLoopLengthTicks();
            private final long checkpointNanos = System.nanoTime();
            private final long checkpointTicks = pianoRollPattern.getTicksAtTime(checkpointNanos);
            private final double nanosPerTick = pianoRollPattern.getNanosPerTick();

            @Override
            public void run() {
                if(currentPositionTrackerId != this.id) {
                    return;
                }
                if(pianoRollPattern.isPlaying()) {
                    long totalTicks = checkpointTicks + (long) ((System.nanoTime() - checkpointNanos) / nanosPerTick);
                    long ticksInCurrentLoop = totalTicks % loopLengthTicks;
                    if(pianoRollPosition != null) {
                        pianoRollPosition.setText(MusicTime.ticksToString(ticksInCurrentLoop));
                        pianoRollPosition.postDelayed(this, 30);
                    }
                }
            }
        };
        pianoRollPosition.postDelayed(tracker, 30);
    }

    /* Child fragments */
    public void registerEditorFragmentNav(IconNavigationPanel panel) {
        panel.setOnItemSelectedListener(new IconNavigationPanel.OnItemSelectedListener() {
            @Override
            public void onSelect(View v) {
                int fragmentId = -1;

                switch(v.getId()) {
                    case R.id.open_pattern_edit_base:
                        fragmentId = AppConstants.PATTERN_EDITOR_BASE;
                        break;
                    case R.id.open_snap_length:
                        fragmentId = AppConstants.PATTERN_EDITOR_SNAP_LENGTH;
                        break;
                    case R.id.open_note_properties:
                        fragmentId = AppConstants.PATTERN_EDITOR_NOTE_PROPERTIES;
                        break;
                    case R.id.open_pattern_length:
                        fragmentId = AppConstants.PATTERN_EDITOR_PATTERN_LENGTH;
                        break;
                    case R.id.open_copy_multiple:
                        fragmentId = AppConstants.PATTERN_EDITOR_COPY_MULTIPLE;
                        break;
                }
                if(fragmentId != -1) {
                    setEditorFragment(fragmentId);
                }
            }
        });
    }

    private static final Map<Class, Integer> navPositionLookup = new HashMap<>();
    static {
        navPositionLookup.put(PatternEditBase.class, AppConstants.PATTERN_EDITOR_BASE);
        navPositionLookup.put(PatternEditNoteProperties.class, AppConstants.PATTERN_EDITOR_NOTE_PROPERTIES);
        navPositionLookup.put(PatternEditCopyMultiple.class, AppConstants.PATTERN_EDITOR_COPY_MULTIPLE);
        navPositionLookup.put(PatternEditSelectSpecial.class, AppConstants.PATTERN_EDITOR_SELECT_SPECIAL);
        navPositionLookup.put(PatternEditSnapLength.class, AppConstants.PATTERN_EDITOR_SNAP_LENGTH);
        navPositionLookup.put(PatternEditPatternLength.class, AppConstants.PATTERN_EDITOR_PATTERN_LENGTH);
    }
    public void setEditorFragment(int which) {
        FragmentManager fm = getChildFragmentManager();
        if(which == AppConstants.PATTERN_EDITOR_BACK && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            return;
        }

        Fragment prevFragment = fm.findFragmentById(R.id.pattern_edit_fragment_container);
        Integer prevWhich = null;
        if(prevFragment != null) {
            prevWhich = navPositionLookup.get(prevFragment.getClass());
            if(prevWhich == null) {
                prevWhich = -1;
            } else if(prevWhich == which) {
                return;
            }
        }

        FragmentTransaction transaction = fm.beginTransaction();
        Fragment fragment = null;
        if(which == AppConstants.PATTERN_EDITOR_BASE) {
            fragment = new PatternEditBase();
        } else if(which == AppConstants.PATTERN_EDITOR_NOTE_PROPERTIES) {
            fragment = new PatternEditNoteProperties();
        } else if(which == AppConstants.PATTERN_EDITOR_COPY_MULTIPLE) {
            fragment = new PatternEditCopyMultiple();
        } else if(which == AppConstants.PATTERN_EDITOR_SELECT_SPECIAL) {
            fragment = new PatternEditSelectSpecial();
        } else if(which == AppConstants.PATTERN_EDITOR_SNAP_LENGTH) {
            fragment = new PatternEditSnapLength();
        } else if(which == AppConstants.PATTERN_EDITOR_PATTERN_LENGTH) {
            fragment = new PatternEditPatternLength();
        } else {
            return;
        }

        transaction.addToBackStack(null);
        if(fragment != null) {
            if(prevWhich != null) {
                if(which < prevWhich) {
                    transaction.setCustomAnimations(R.anim.slide_in_from_top, 0);
                } else {
                    transaction.setCustomAnimations(R.anim.slide_in_from_bottom, 0);
                }
            }
            transaction.replace(R.id.pattern_edit_fragment_container, fragment);
            transaction.commit();
        }
    }

    public interface PianoRollTapListener {
        void onSingleTap(MusicTime xTime, int keyNum);
    }
}
