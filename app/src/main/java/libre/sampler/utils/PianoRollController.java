package libre.sampler.utils;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.TextAppearanceSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.core.util.Consumer;
import libre.sampler.R;
import libre.sampler.adapters.PianoRollAdapter;
import libre.sampler.listeners.StatefulScrollListener;
import libre.sampler.models.Instrument;
import libre.sampler.models.InstrumentEvent;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.Pattern;
import libre.sampler.models.PatternEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.views.PianoRollNoteView;

public class PianoRollController {
    private ProjectViewModel viewModel;
    private final PatternThread patternThread;
    private Pattern activePattern;
    private Instrument pianoRollInstrument;

    public PianoRollAdapter adapter;

    public int baseBarWidth;
    public float keyHeight;

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

    public PianoRollController(ProjectViewModel viewModel, PatternThread patternThread) {
        this.viewModel = viewModel;
        this.patternThread = patternThread;
        this.activePattern = viewModel.getPianoRollPattern();
        this.pianoRollInstrument = viewModel.getKeyboardInstrument();

        this.noteLength = new MusicTime(0, 4, 0);
        this.snap = new MusicTime(0, 1, 0);
        this.velocity = 100;
        this.inputLoopLength = Pattern.DEFAULT_LOOP_LENGTH;
        this.inputTempo = Pattern.DEFAULT_TEMPO;

        viewModel.patternEventSource.add("PianoRollController", new Consumer<PatternEvent>() {
            @Override
            public void accept(PatternEvent event) {
                if(event.action == PatternEvent.PATTERN_SELECT || event.action == PatternEvent.PATTERN_CREATE_SELECT) {
                    PianoRollController.this.activePattern = event.pattern;
                    updatePatternLengthInputs();
                    updateTempoInput();
                }
            }
        });

        viewModel.instrumentEventSource.add("PianoRollController", new Consumer<InstrumentEvent>() {
            @Override
            public void accept(InstrumentEvent event) {
                if(event.action == InstrumentEvent.INSTRUMENT_DELETE ||
                        event.action == InstrumentEvent.INSTRUMENT_CREATE ||
                        event.action == InstrumentEvent.INSTRUMENT_EDIT) {
                    instrumentSpinnerItems = PianoRollController.this.viewModel.getProject().getInstruments();
                    updateInstrumentInput();
                }
            }
        });

        this.inputLoopLength.setTicks(activePattern.getLoopLengthTicks());
        this.inputTempo = activePattern.getTempo();

        this.adapter = new PianoRollAdapter(this);
    }

    // Called from PatternsFragment once layout dimensions are set
    public void setPianoRollNotes(Context ctx) {
        double tickWidth = getTickWidth();

        Map<Long, PianoRollNoteView> noteViews = new HashMap<>();
        for(ScheduledNoteEvent event : activePattern.events) {
            if(event.action == NoteEvent.NOTE_ON) {
                PianoRollNoteView v = new PianoRollNoteView(ctx);
                noteViews.put(event.noteId, v);
                v.eventOn = event;
            } else if(event.action == NoteEvent.NOTE_OFF) {
                PianoRollNoteView v = noteViews.get(event.noteId);
                if(v != null) {
                    v.eventOff = event;
                    v.containerIndex = 9 - (event.keyNum / 12);
                    int keyIndex = 11 - (event.keyNum % 12);
                    long noteLengthTicks = event.offsetTicks - v.eventOn.offsetTicks;
                    v.layoutParams = new RelativeLayout.LayoutParams((int) (noteLengthTicks * tickWidth), (int) keyHeight);
                    v.layoutParams.leftMargin = (int) (v.eventOn.offsetTicks * tickWidth);
                    v.layoutParams.topMargin = (int) (keyIndex * keyHeight);
                    v.setBackgroundColor(Color.WHITE);
                }
            }
        }

        adapter.setPianoRollNotes(new ArrayList<>(noteViews.values()));
        adapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
    }

    public void onCreatePianoRollNote(PianoRollNoteView outView, int containerIndex, float x, float y) {
        outView.containerIndex = containerIndex;

        double tickWidth = getTickWidth();
        double inputTicks = x / tickWidth;
        long snapTicks = snap.getTicks();

        long startTicks = Math.round(inputTicks / 1.0 / snapTicks - 0.3) * snapTicks;

        long maxTicks = activePattern.getLoopLengthTicks() - startTicks;
        long noteLengthTicks = noteLength.getTicks();
        if(noteLengthTicks > maxTicks) {
            noteLengthTicks = (long) Math.floor(maxTicks / 1.0 / snapTicks) * snapTicks;
        }

        int keyIndex = (int) (y / keyHeight);
        outView.layoutParams = new RelativeLayout.LayoutParams((int) (noteLengthTicks * tickWidth), (int) keyHeight);
        outView.layoutParams.leftMargin = (int) (startTicks * tickWidth);
        outView.layoutParams.topMargin = (int) (keyIndex * keyHeight);
        outView.setBackgroundColor(Color.WHITE);

        int keyNum = (9 - containerIndex) * 12 + (11 - keyIndex);
        long baseId = AppConstants.PATTERN_EVENT_ID_OFFSET + System.currentTimeMillis();
        outView.eventOn = new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                pianoRollInstrument, keyNum, velocity, baseId);
        outView.eventOff = new ScheduledNoteEvent(startTicks + noteLengthTicks, NoteEvent.NOTE_OFF,
                pianoRollInstrument, keyNum, velocity, baseId);
        addToActivePattern(outView.eventOn, outView.eventOff);
    }

    public void onRemovePianoRollNote(PianoRollNoteView outView) {
        if(outView.eventOn != null && outView.eventOff != null) {
            removeFromActivePattern(outView.eventOn, outView.eventOff);
        }
    }

    private void addToActivePattern(ScheduledNoteEvent... noteEvents) {
        patternThread.lock.lock();
        try {
            for(ScheduledNoteEvent noteEvent : noteEvents) {
                int insertIdx = Collections.binarySearch(activePattern.events, noteEvent, new Comparator<ScheduledNoteEvent>() {
                    @Override
                    public int compare(ScheduledNoteEvent o1, ScheduledNoteEvent o2) {
                        return Long.compare(o1.offsetTicks, o2.offsetTicks);
                    }
                });
                if(insertIdx < 0) {
                    insertIdx = -insertIdx - 1;
                }
                activePattern.addEvent(insertIdx, noteEvent);
            }
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    private void removeFromActivePattern(ScheduledNoteEvent noteEventOn, ScheduledNoteEvent noteEventOff) {
        patternThread.lock.lock();
        try {
            activePattern.removeEvent(noteEventOn);
            NoteEvent sendOff = activePattern.removeAndGetEvent(noteEventOff);
            if(sendOff != null) {
                patternThread.noteEventSource.dispatch(sendOff);
            }
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    public Pattern getActivePattern() {
        return activePattern;
    }

    public double getTickWidth() {
        return baseBarWidth / 1.0 / MusicTime.TICKS_PER_BAR;
    }

    public void setLoopLength(MusicTime loopLength) {
        this.inputLoopLength = loopLength;
        patternThread.lock.lock();
        try {
            activePattern.setLoopLengthTicks(loopLength.getTicks());
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
        adapter.updateRollLength((int) (loopLength.getTicks() * getTickWidth()));
    }

    public void setTempo(double bpm) {
        this.inputTempo = bpm;
        patternThread.lock.lock();
        try {
            activePattern.setTempo(bpm);
            patternThread.notifyPatternsChanged();
        } finally {
            patternThread.lock.unlock();
        }
    }

    public void registerInstrumentInput(Spinner spinner) {
        this.instrumentSpinner = spinner;
        this.instrumentSpinnerAdapter = new ArrayAdapter<>(
                instrumentSpinner.getContext(), R.layout.component_spinner_item);
        this.instrumentSpinnerItems = viewModel.getProject().getInstruments();

        instrumentSpinnerAdapter.setDropDownViewResource(R.layout.component_spinner_dropdown_item);
        instrumentSpinner.setAdapter(instrumentSpinnerAdapter);

        instrumentSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pianoRollInstrument = instrumentSpinnerItems.get(position);
                viewModel.instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_LOAD, pianoRollInstrument));
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

    public void registerSnapInput(Spinner spinner) {
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
                spinner.getContext(), R.layout.component_spinner_item, options);
        spinnerAdapter.setDropDownViewResource(R.layout.component_spinner_dropdown_item);
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
                setLoopLength(inputLoopLength);
                adapter.updateRollLength((int) (inputLoopLength.getTicks() * getTickWidth()));
            }
        });

        updatePatternLengthInputs();
    }
    
    private void updatePatternLengthInputs() {
        if(loopLengthPickerBars != null && loopLengthPickerSixteenths != null) {
            if(activePattern != null) {
                inputLoopLength.setTicks(activePattern.getLoopLengthTicks());
            }
            loopLengthPickerBars.setValue(inputLoopLength.bars);
            loopLengthPickerSixteenths.setValue(inputLoopLength.sixteenths);
        }
    }

    public void registerTempoInput(EditText tempoEditText) {
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
                    setTempo(inputTempo);
                }
                return false;
            }
        });

        updateTempoInput();
    }

    private void updateTempoInput() {
        if(tempoEditText != null) {
            if(activePattern != null) {
                inputTempo = activePattern.getTempo();
            }
            DecimalFormat fmt = new DecimalFormat("0.###");
            tempoEditText.setText(fmt.format(inputTempo));
        }
    }
}
