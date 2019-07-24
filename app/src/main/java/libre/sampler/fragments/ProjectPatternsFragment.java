package libre.sampler.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import java.util.ArrayList;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.fragments.patternedit.PatternEditBase;
import libre.sampler.fragments.patternedit.PatternEditCopyMultiple;
import libre.sampler.fragments.patternedit.PatternEditNoteProperties;
import libre.sampler.fragments.patternedit.PatternEditPatternLength;
import libre.sampler.fragments.patternedit.PatternEditSelectSpecial;
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
import libre.sampler.utils.PatternThread;
import libre.sampler.views.IconNavigationPanel;
import libre.sampler.views.VisualNote;

public class ProjectPatternsFragment extends Fragment {
    public static final String TAG = "ProjectPatternsFragment";
    private ProjectViewModel viewModel;
    private ProjectActivity projectActivity;
    private PatternDerivedData patternDerivedData;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private final TreeSet<VisualNote> selectedNotes = new TreeSet<>();
    public EmptyEventSource patternEditEventSource = new EmptyEventSource();
    private List<PianoRollTapListener> pianoRollTapListeners = new ArrayList<>();

    private double tickWidth;
    private float keyHeight;

    private MusicTime inputNoteLength;
    private MusicTime snap;
    private MusicTime inputLoopLength;
    private double inputTempo;
    public int velocity;

    private TextView pianoRollPosition;
    private Rect tmpRect = new Rect();
    private MusicTime visibleLeft = new MusicTime(0L);
    private MusicTime visibleRight = new MusicTime(0L);
    private int visibleTop;     // midi note
    private int visibleBottom;  // midi note

    private EditText tempoEditText;
    private ImageView patternStop;
    private ImageView patternPlay;
    private RecyclerViewScrollChangeDispatcher scrollChangeDispatcher;

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
        setPianoRollNotes();

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

        scrollChangeDispatcher = new RecyclerViewScrollChangeDispatcher(pianoRollContainer);
        scrollChangeDispatcher.setListener(new ScrollChangeListener() {
            @Override
            public void onScrollChange(int x, int y, int oldX, int oldY) {
                updatePianoRollPosition(x);
                updateVisibleRect(x, y);
            }
        });
        updatePianoRollPosition(0);

        updatePlayPauseControls(getContext());

        registerEditorFragmentNav((IconNavigationPanel) rootView.findViewById(R.id.pattern_edit_nav));
        setEditorFragment(AppConstants.PATTERN_EDITOR_BASE);

        return rootView;
    }

    private void updateVisibleRect(int x, int y) {
        int dx = pianoRollContainer.getWidth();
        pianoRollContainer.getLocalVisibleRect(tmpRect);
        int dy = tmpRect.height();
        visibleLeft.setTicks((long) (x / 1.0 / tickWidth));
        visibleRight.setTicks((long) ((x + dx) / 1.0 / tickWidth));
        visibleTop = AppConstants.PIANO_ROLL_TOP_KEYNUM - (int) Math.floor(y / keyHeight);
        visibleBottom = AppConstants.PIANO_ROLL_TOP_KEYNUM - (int) Math.ceil((y + dy) / keyHeight) + 1;
    }

    public MusicTime getVisibleLeft() {
        return visibleLeft.copy();
    }

    public MusicTime getVisibleRight() {
        return visibleRight.copy();
    }

    public int getVisibleTop() {
        return visibleTop;
    }

    public int getVisibleBottom() {
        return visibleBottom;
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
        pianoRollContainer.setAdapter(null);
        scrollChangeDispatcher.onDestroyView();
        super.onDestroyView();
        this.scrollChangeDispatcher = null;
        this.pianoRollContainer = null;
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
     * PianoRollAdapter.
     *
     * <p>Changes to the properties of notes are handled by {@link PianoRollAdapter#updateNote} or
     * {@link PianoRollAdapter#updateAllNotes}
     */
    private void setPianoRollNotes() {
        Instrument pianoRollInstrument = viewModel.getPianoRollInstrument();
        if(pianoRollInstrument != null) {
            List<VisualNote> noteViews = patternDerivedData.getNotesForInstrument(pianoRollInstrument);
            pianoRollAdapter.setPianoRollNotes(noteViews);
        }
    }

    public void onAdapterCreateNote(int containerIndex, float x, float y) {
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
        VisualNote visualNote = createVisualNote(startTicks, noteLengthTicks, keyNum, baseId);
        addToPianoRollPattern(visualNote);
        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
    }

    public void onAdapterRemoveNote(VisualNote visualNote) {
        removeFromPianoRollPattern(visualNote);
        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
        if(selectedNotes.remove(visualNote)) {
            patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
        }
    }

    private VisualNote createVisualNote(long startTicks, long lengthTicks, int keyNum, long baseId) {
        VisualNote visualNote = new VisualNote();
        visualNote.setEventOn(new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId));
        visualNote.setEventOff(new ScheduledNoteEvent(startTicks + lengthTicks, NoteEvent.NOTE_OFF,
                viewModel.getPianoRollInstrument(), keyNum, velocity, baseId));
        return visualNote;
    }


    private void addToPianoRollPattern(VisualNote visualNote) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.addEvent(visualNote.eventOn);
            editor.pattern.addEvent(visualNote.eventOff);
            pianoRollAdapter.addNote(visualNote, selectedNotes.contains(visualNote));
        }
    }

    private void removeFromPianoRollPattern(VisualNote visualNote) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.removeEvent(visualNote.eventOn);
            NoteEvent sendOff = editor.pattern.removeAndGetEvent(visualNote.eventOff);
            if(sendOff != null) {
                viewModel.noteEventSource.dispatch(sendOff);
            }
            pianoRollAdapter.removeNote(visualNote);
        }
    }

    public void insertMultiplePianoRollNotes(VisualNote src, MusicTime interval, int count) {
        long intervalTicks = interval.getTicks();
        long baseId = NoteId.createForScheduledNoteEvent(System.currentTimeMillis(), 0);
        for(int i = 1; i <= count; i++) {
            long startTicks = src.startTicks + intervalTicks * i;
            VisualNote visualNote = new VisualNote();
            visualNote.setEventOn(new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                    src.eventOn.instrument, src.eventOn.keyNum, src.eventOn.velocity, baseId));
            visualNote.setEventOff(new ScheduledNoteEvent(startTicks + src.lengthTicks, NoteEvent.NOTE_OFF,
                    src.eventOff.instrument, src.eventOff.keyNum, src.eventOff.velocity, baseId));

            addToPianoRollPattern(visualNote);
            baseId = NoteId.createDuplicate(baseId);
        }
        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
    }

    public void deleteAllSelectedNotes() {
        for(VisualNote note : selectedNotes) {
            removeFromPianoRollPattern(note);
        }
        selectedNotes.clear();
        patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public boolean onAdapterSelectNote(VisualNote n) {
        if(selectedNotes.remove(n)) {
            patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
            return false;
        }
        selectedNotes.add(n);
        inputNoteLength.setTicks(n.lengthTicks);
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
        return true;
    }

    public TreeSet<VisualNote> getSelectedNotes() {
        return selectedNotes;
    }

    public boolean isSelectedNotesComplete() {
        return (pianoRollAdapter.getPianoRollNotes().size() == selectedNotes.size());
    }

    public void clearSelectedNotes() {
        selectedNotes.clear();
        pianoRollAdapter.updateAllNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }
    
    public void selectAllNotes() {
        selectedNotes.addAll(pianoRollAdapter.getPianoRollNotes());
        pianoRollAdapter.updateAllNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
    }

    public void selectNotesInRect(int top, MusicTime left, int bottom, MusicTime right) {
        selectedNotes.clear();

        long rightTicks = right.getTicks();
        long leftTicks = left.getTicks();
        for(VisualNote note : pianoRollAdapter.getPianoRollNotes()) {
            if(note.eventOn.keyNum <= top && note.eventOn.keyNum >= bottom &&
                    note.startTicks <= rightTicks && (note.startTicks + note.lengthTicks >= leftTicks)) {

                selectedNotes.add(note);
            }
        }
        pianoRollAdapter.updateAllNotes();
        patternEditEventSource.dispatch(AppConstants.SELECTED_NOTES);
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
                        }
                    }
                }, 50);
            }
        }
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
                        addToPianoRollPattern(noteRef);
                    }
                }
            }, 50);
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

    public void setPatternLength(MusicTime inputLoopLength) {
        try(PatternThread.Editor editor = projectActivity.getPatternThread()
                .getEditor(viewModel.getPianoRollPattern())) {

            editor.pattern.setLoopLengthTicks(inputLoopLength.getTicks());
            pianoRollAdapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
        }
    }

    public void addPianoRollTapListener(PianoRollTapListener listener) {
        pianoRollTapListeners.add(listener);
    }

    public void removePianoRollTapListener(PianoRollTapListener listener) {
        pianoRollTapListeners.remove(listener);
    }

    public void dispatchPianoRollTap(int containerIndex, float x, float y) {
        MusicTime xTime = new MusicTime((long) (x / tickWidth));
        int keyIndex = (int) (y / keyHeight);
        int keyNum = (9 - containerIndex) * 12 + (11 - keyIndex);

        for(PianoRollTapListener fn : pianoRollTapListeners) {
            fn.onSingleTap(xTime, keyNum);
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
                    try(PatternThread.Editor editor = projectActivity.getPatternThread()
                            .getEditor(viewModel.getPianoRollPattern())) {

                        editor.pattern.setTempo(inputTempo);
                    }
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

        if(prevWhich != null) {
            Slide trIn = new Slide(Gravity.BOTTOM);
            trIn.setPropagation(new NoTransitionPropagation());
            fragment.setEnterTransition(trIn);
            fragment.setReenterTransition(trIn);
            if(which < prevWhich) {
                trIn.setSlideEdge(Gravity.TOP);
            }
        }

        transaction.addToBackStack(null);
        if(fragment != null) {
            transaction.replace(R.id.pattern_edit_fragment_container, fragment);
            transaction.commit();
        }
    }

    public interface PianoRollTapListener {
        void onSingleTap(MusicTime xTime, int keyNum);
    }
}
