package libre.sampler.models;

import android.app.Application;
import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import libre.sampler.publishers.EmptyEventSource;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.tasks.LoadProjectTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.ModelState;
import libre.sampler.views.VisualNote;

public class ProjectViewModel extends AndroidViewModel {
    public String projectId = null;

    private Project project;
    private ModelState projectState = ModelState.INVALID;
    private byte[] projectHash;

    private Instrument keyboardInstrument;
    private Sample editorSample;

    private Instrument pianoRollInstrument;
    private Pattern pianoRollPattern;
    private HashMap<Pattern, PatternDerivedData> patternDerivedDataCache = new HashMap<>();

    private Instrument dialogInstrument;

    private MidiEventDispatcher midiEventDispatcher;

    public final EmptyEventSource loadEventSource = new EmptyEventSource();
    public final NoteEventSource noteEventSource = new NoteEventSource();
    public final InstrumentEventSource instrumentEventSource = new InstrumentEventSource();
    public final PatternEventSource patternEventSource = new PatternEventSource();

    public ProjectViewModel(@NonNull Application application) {
        super(application);
        DatabaseConnectionManager.initialize(application);
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    @Nullable
    public Project tryGetProject() {
        if(project == null && projectState == ModelState.INVALID) {
            if(projectId == null) {
                throw new AssertionError("Project ID not set");
            }

            projectState = ModelState.LOADING;
            DatabaseConnectionManager.runTask(new LoadProjectTask(projectId,
                    new Consumer<LoadProjectTask.ProjectData>() {
                        @Override
                        public void accept(LoadProjectTask.ProjectData data) {
                            ProjectViewModel.this.project = data.project;
                            project.setInstruments(data.instruments);
                            if(data.instruments.size() > 0) {
                                keyboardInstrument = data.instruments.get(0);
                            }
                            project.setPatterns(data.patterns);
                            if(data.patterns.size() > 0) {
                                pianoRollPattern = data.patterns.get(0);
                            } else {
                                pianoRollPattern = Pattern.getEmptyPattern();
                                project.addPattern(pianoRollPattern);
                            }
                            loadEventSource.dispatch(AppConstants.INSTRUMENTS_PATTERNS_LOADED);
                            if(keyboardInstrument != null) {
                                instrumentEventSource.dispatch(new InstrumentEvent(
                                        InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT, keyboardInstrument));
                            }
                            projectHash = project.valueHash();
                            projectState = ModelState.LOADED;
                        }
                    })
            );
        }

        return project;
    }

    @NonNull
    public Project getProject() {
        return Objects.requireNonNull(project);
    }

    public boolean projectHasUnsavedChanges() {
        if(projectState == ModelState.LOADED) {
            byte[] currentHash = project.valueHash();

            if(projectHash == null || currentHash == null) {
                return true;
            }

            return !Arrays.equals(projectHash, currentHash);
        }
        return false;
    }

    public void prepareSave() {
        project.prepareSave();
        projectHash = project.valueHash();
    }

    public Instrument getKeyboardInstrument() {
        return keyboardInstrument;
    }

    public void setKeyboardInstrument(Instrument keyboardInstrument) {
        this.keyboardInstrument = keyboardInstrument;
        this.editorSample = null;
        if(keyboardInstrument != null) {
            instrumentEventSource.dispatch(new InstrumentEvent(
                    InstrumentEvent.INSTRUMENT_KEYBOARD_SELECT, keyboardInstrument));
        }
    }

    public Pattern getPianoRollPattern() {
        return pianoRollPattern;
    }

    public void setPianoRollPattern(Pattern pianoRollPattern) {
        this.pianoRollPattern = pianoRollPattern;
        patternEventSource.dispatch(new PatternEvent(PatternEvent.PATTERN_SELECT, pianoRollPattern));
    }

    public MidiEventDispatcher getMidiEventDispatcher() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MidiManager midiManager = (MidiManager) getApplication().getSystemService(Context.MIDI_SERVICE);
            if(midiManager == null) {
                return null;
            }

            MidiDeviceInfo[] midiDeviceInfos = midiManager.getDevices();
            if(midiDeviceInfos.length > 0) {
                if(midiEventDispatcher == null) {
                    midiEventDispatcher = new MidiEventDispatcher(this);
                }
                midiManager.openDevice(midiDeviceInfos[0], midiEventDispatcher, new Handler());
            } else {
                if(midiEventDispatcher != null) {
                    midiEventDispatcher.closeMidi();
                    midiEventDispatcher = null;
                }
            }
        }

        return midiEventDispatcher;
    }

    public PatternDerivedData getPatternDerivedData(Pattern pattern) {
        // todo async
        PatternDerivedData derivedData = patternDerivedDataCache.get(pattern);
        if(derivedData != null) {
            return derivedData;
        }

        Map<Instrument, List<VisualNote>> derivedMap = new HashMap<>();
        Map<Long, ScheduledNoteEvent> eventOnMap = new HashMap<>();
        Set<Long> eventOffKeySet = new HashSet<>();

        for(ScheduledNoteEvent event : pattern.getEventsDeepCopy()) {
            if(event.action == NoteEvent.NOTE_ON) {
                ScheduledNoteEvent prevVal = eventOnMap.put(event.noteId, event);
                if(prevVal != null) {
                    Log.e("Sampler", String.format("duplicate note ID:\n  %s\n  %s",
                            prevVal.toString(), event.toString()));
                }
            } else if(event.action == NoteEvent.NOTE_OFF) {
                eventOffKeySet.add(event.noteId);
                ScheduledNoteEvent eventOn = eventOnMap.get(event.noteId);
                if(eventOn == null) {
                    Log.e("Sampler", String.format("unpaired NOTE_OFF:\n  %s",
                            event.toString()));
                    continue;
                }

                VisualNote v = new VisualNote(eventOn, event);
                List<VisualNote> viewsForInstrument = derivedMap.get(event.instrument);
                if(viewsForInstrument == null) {
                    viewsForInstrument = new ArrayList<>();
                    derivedMap.put(event.instrument, viewsForInstrument);
                }
                viewsForInstrument.add(v);
            }
        }

        if(!eventOffKeySet.containsAll(eventOnMap.keySet())) {
            Log.e("Sampler", "unpaired NOTE_ON");
        }

        derivedData = new PatternDerivedData(derivedMap);
        patternDerivedDataCache.put(pattern, derivedData);
        return derivedData;
    }

    public Sample getEditorSample() {
        if(editorSample == null) {
            if(keyboardInstrument != null && keyboardInstrument.getSamples().size() > 0) {
                editorSample = keyboardInstrument.getSamples().get(0);
            }
        }
        return editorSample;
    }

    public void setEditorSample(Sample editorSample) {
        this.editorSample = editorSample;
    }

    public Instrument getPianoRollInstrument() {
        return pianoRollInstrument;
    }

    public void setPianoRollInstrument(Instrument pianoRollInstrument) {
        if(this.pianoRollInstrument != pianoRollInstrument) {
            this.pianoRollInstrument = pianoRollInstrument;
            if(pianoRollInstrument != null) {
                instrumentEventSource.dispatch(new InstrumentEvent(
                        InstrumentEvent.INSTRUMENT_PIANO_ROLL_SELECT, pianoRollInstrument));
            }
        }
    }

    public Instrument getDialogInstrument() {
        return this.dialogInstrument;
    }

    public void setDialogInstrument(Instrument instrument) {
        this.dialogInstrument = instrument;
    }

    public void removeInstrument(Instrument toRemove) {
        if(projectState != ModelState.LOADED) {
            return;
        }

        instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_PREDELETE, toRemove));
        project.removeInstrument(toRemove);
        instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_DELETE, toRemove));
    }
}
