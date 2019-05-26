package libre.sampler.fragments;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TextAppearanceSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.PatternDerivedData;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.PatternLoader;
import libre.sampler.utils.PatternThread;
import libre.sampler.views.VisualNote;

public class ProjectPatternsFragment extends Fragment {
    private ProjectViewModel viewModel;
    private PatternThread patternThread;
    private PatternDerivedData patternDerivedData;
    private Instrument pianoRollInstrument;

    private RecyclerView pianoRollContainer;
    private PianoRollAdapter pianoRollAdapter;
    private PatternLoader patternLoader;

    private double tickWidth;
    private float keyHeight;

    private static final int MAX_INPUT_BARS = 99;
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

    public MusicTime noteLength;
    public MusicTime snap;
    public MusicTime inputLoopLength;
    public double inputTempo;
    public int velocity;

    private Spinner instrumentSpinner;
    private ArrayAdapter<String> instrumentSpinnerAdapter;
    private List<Instrument> instrumentSpinnerItems;

    private NumberPicker loopLengthPickerBars;
    private NumberPicker loopLengthPickerSixteenths;
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

        patternLoader = new PatternLoader(patternThread);
        this.keyHeight = getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        this.tickWidth = getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth) / 1.0 / MusicTime.TICKS_PER_BAR;
        this.noteLength = new MusicTime(0, 4, 0);
        this.snap = new MusicTime(0, 1, 0);
        this.velocity = 100;
        this.inputLoopLength = new MusicTime(viewModel.getPianoRollPattern().getLoopLengthTicks());
        this.inputTempo = viewModel.getPianoRollPattern().getTempo();

        registerInstrumentInput((Spinner) rootView.findViewById(R.id.piano_roll_settings_instrument));
        registerNoteLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length1),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_length2));
        registerSnapInput((Spinner) rootView.findViewById(R.id.piano_roll_settings_snaplength0));
        registerPatternLengthInputs((NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength0),
                (NumberPicker) rootView.findViewById(R.id.piano_roll_settings_patternlength1),
                (Button) rootView.findViewById(R.id.submit_patternlength));
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
                    updatePatternLengthInputs();
                    updateTempoInput();
                }
            }
        });

        viewModel.instrumentEventSource.add("PatternLoader", new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_DELETE ||
                        event.action == InstrumentEvent.INSTRUMENT_CREATE ||
                        event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                    instrumentSpinnerItems = viewModel.getProject().getInstruments();
                    updateInstrumentInput();
                }
            }
        });

        patternDerivedData = viewModel.getPatternDerivedData(viewModel.getPianoRollPattern());
        for(Instrument t : patternDerivedData.getInstrumentList()) {
            viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, t));
        }
        if(!patternDerivedData.getInstrumentList().isEmpty()) {
            pianoRollInstrument = patternDerivedData.getInstrumentList().get(0);
        } else {
            pianoRollInstrument = viewModel.getKeyboardInstrument();
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
                pianoRollInstrument, keyNum, velocity, baseId);
        outView.eventOff = new ScheduledNoteEvent(startTicks + noteLengthTicks, NoteEvent.NOTE_OFF,
                pianoRollInstrument, keyNum, velocity, baseId);

        outView.calculateParams();
        patternLoader.addToPattern(viewModel.getPianoRollPattern(), outView.eventOn, outView.eventOff);
    }

    public void onRemovePianoRollNote(VisualNote outView) {
        if(outView.eventOn != null && outView.eventOff != null) {
            patternLoader.removeFromPattern(viewModel.getPianoRollPattern(), outView.eventOn, outView.eventOff);
        }
    }

    private void updatePianoRollNotes() {
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

    private void registerInstrumentInput(Spinner spinner) {
        this.instrumentSpinner = spinner;
        this.instrumentSpinnerAdapter = new ArrayAdapter<>(
                instrumentSpinner.getContext(), android.R.layout.simple_spinner_item);
        this.instrumentSpinnerItems = viewModel.getProject().getInstruments();

        instrumentSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        instrumentSpinner.setAdapter(instrumentSpinnerAdapter);

        instrumentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pianoRollInstrument = instrumentSpinnerItems.get(position);
                updatePianoRollNotes();
                viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PD_LOAD, pianoRollInstrument));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        updateInstrumentInput();
    }

    private void updateInstrumentInput() {
        if(instrumentSpinner != null && instrumentSpinnerAdapter != null) {
            String[] options = new String[instrumentSpinnerItems.size()];
            for(int i = 0; i < options.length; i++) {
                options[i] = instrumentSpinnerItems.get(i).name;
            }

            instrumentSpinnerAdapter.clear();
            instrumentSpinnerAdapter.addAll(options);
            instrumentSpinner.setSelection(instrumentSpinnerItems.indexOf(pianoRollInstrument));
        }
    }

    public void registerNoteLengthInputs(final NumberPicker pickerBars, final NumberPicker pickerSixteenths, NumberPicker pickerUserTicks) {
        final StatefulScrollListener pickerUserTicksScrolling = new StatefulScrollListener();
        pickerUserTicks.setOnScrollListener(pickerUserTicksScrolling);
        pickerUserTicks.setMinValue(0);
        pickerUserTicks.setMaxValue(MusicTime.USER_TICKS_PER_SIXTEENTH - 1);
        pickerUserTicks.setValue(noteLength.userTicks);
        pickerUserTicks.setFormatter(new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return String.format("%02d", value);
            }
        });
        pickerUserTicks.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                noteLength.userTicks = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        noteLength.sixteenths += 1;
                        pickerSixteenths.setValue(noteLength.sixteenths);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerUserTicksScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerSixteenths.getValue() > pickerSixteenths.getMinValue()) {
                        noteLength.sixteenths -= 1;
                        pickerSixteenths.setValue(noteLength.sixteenths);
                    }
                }
            }
        });

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setValue(noteLength.sixteenths);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                noteLength.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        noteLength.bars += 1;
                        pickerBars.setValue(noteLength.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        noteLength.bars -= 1;
                        pickerBars.setValue(noteLength.bars);
                    }
                }
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setValue(noteLength.bars);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                noteLength.bars = newVal;
            }
        });

        pickerUserTicks.setWrapSelectorWheel(true);
        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);
    }

    private void registerSnapInput(Spinner spinner) {
        int currentSelection = 0;
        if(snap == null) {
            currentSelection = SNAP_OPTIONS.length - 1;
        } else {
            for(/* int currentSelection = 0 */; currentSelection < SNAP_OPTIONS.length; currentSelection++) {
                if(SNAP_OPTIONS[currentSelection].getTicks() <= snap.getTicks()) {
                    break;
                }
            }
        }

        SpannableString[] options = new SpannableString[SNAP_OPTION_DESCRIPTIONS.length];
        for(int i = 0; i < SNAP_OPTION_DESCRIPTIONS.length; i++) {
            options[i] = new SpannableString(SNAP_OPTION_DESCRIPTIONS[i]);
            options[i].setSpan(new TextAppearanceSpan(spinner.getContext(), R.style.TextAppearanceMonospace),
                    SNAP_OPTION_DESCRIPTIONS[i].indexOf("|") + 2, SNAP_OPTION_DESCRIPTIONS[i].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        ArrayAdapter<SpannableString> spinnerAdapter = new ArrayAdapter<>(
                spinner.getContext(), android.R.layout.simple_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(currentSelection);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                snap = SNAP_OPTIONS[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void registerPatternLengthInputs(final NumberPicker pickerBars, NumberPicker pickerSixteenths, Button confirmButton) {
        this.loopLengthPickerBars = pickerBars;
        this.loopLengthPickerSixteenths = pickerSixteenths;

        final StatefulScrollListener pickerSixteenthsScrolling = new StatefulScrollListener();
        pickerSixteenths.setOnScrollListener(pickerSixteenthsScrolling);
        pickerSixteenths.setMinValue(0);
        pickerSixteenths.setMaxValue(MusicTime.SIXTEENTHS_PER_BAR - 1);
        pickerSixteenths.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputLoopLength.sixteenths = newVal;
                if(oldVal == picker.getMaxValue() && newVal == picker.getMinValue()) {
                    // rollover +
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE) {
                        inputLoopLength.bars += 1;
                        pickerBars.setValue(inputLoopLength.bars);
                    }
                } else if(oldVal == picker.getMinValue() && newVal == picker.getMaxValue()) {
                    // rollover -
                    if(pickerSixteenthsScrolling.scrollState != NumberPicker.OnScrollListener.SCROLL_STATE_IDLE &&
                            pickerBars.getValue() > pickerBars.getMinValue()) {
                        inputLoopLength.bars -= 1;
                        pickerBars.setValue(inputLoopLength.bars);
                    }
                }
            }
        });

        pickerBars.setMinValue(0);
        pickerBars.setMaxValue(MAX_INPUT_BARS);
        pickerBars.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                inputLoopLength.bars = newVal;
            }
        });

        pickerSixteenths.setWrapSelectorWheel(true);
        pickerBars.setWrapSelectorWheel(false);

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                patternLoader.setLoopLength(viewModel.getPianoRollPattern(), inputLoopLength);
                pianoRollAdapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
            }
        });

        updatePatternLengthInputs();
    }

    private void updatePatternLengthInputs() {
        if(loopLengthPickerBars != null && loopLengthPickerSixteenths != null) {
            if(viewModel.getPianoRollPattern() != null) {
                inputLoopLength.setTicks(viewModel.getPianoRollPattern().getLoopLengthTicks());
            }
            loopLengthPickerBars.setValue(inputLoopLength.bars);
            loopLengthPickerSixteenths.setValue(inputLoopLength.sixteenths);
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
}
