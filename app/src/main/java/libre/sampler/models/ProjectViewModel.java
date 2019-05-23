package libre.sampler.models;

import android.app.Application;
import android.content.Context;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Build;
import android.os.Handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.publishers.MidiEventDispatcher;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.publishers.ProjectEventSource;
import libre.sampler.tasks.GetInstrumentsTask;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.views.VisualNote;

public class ProjectViewModel extends AndroidViewModel {
    public int projectId = -1;
    // public String projectName = "";

    private Project project;
    private Instrument keyboardInstrument;

    private Pattern pianoRollPattern;
    private HashMap<Pattern, PatternDerivedData> patternDerivedDataCache = new HashMap<>();

    private Instrument createDialogInstrument;
    private Instrument editDialogInstrument;

    private MidiEventDispatcher midiEventDispatcher;

    public final ProjectEventSource projectEventSource = new ProjectEventSource();
    public final NoteEventSource keyboardNoteSource = new NoteEventSource();
    public final NoteEventSource noteEventSource = new NoteEventSource();
    public final InstrumentEventSource instrumentEventSource = new InstrumentEventSource();
    public final PatternEventSource patternEventSource = new PatternEventSource();

    public ProjectViewModel(@NonNull Application application) {
        super(application);
    }

    public void setProjectId(int projectId) {
        this.projectId = projectId;
    }

    public Project getProject() {
        if(project == null) {
            if(projectId < 0) {
                throw new AssertionError("Project ID not set");
            }

            DatabaseConnectionManager.initialize(getApplication());
            DatabaseConnectionManager.runTask(new GetInstrumentsTask(projectId,
                    new Consumer<Project>() {
                        @Override
                        public void accept(Project project) {
                            ProjectViewModel.this.project = project;
                        }
                    }, new Consumer<List<Instrument>>() {
                        @Override
                        public void accept(List<Instrument> instruments) {
                            project.setInstruments(instruments);
                            if(instruments.size() > 0) {
                                keyboardInstrument = instruments.get(0);
                            }
                        }
                    }, new Consumer<List<Pattern>>() {
                        @Override
                        public void accept(List<Pattern> patterns) {
                            project.setPatterns(patterns);
                            if(patterns.size() > 0) {
                                pianoRollPattern = patterns.get(0);
                            } else {
                                pianoRollPattern = Pattern.getEmptyPattern();
                                project.registerPattern(pianoRollPattern);
                                project.addPattern(pianoRollPattern);
                            }
                            projectEventSource.dispatch(project);
                            if(keyboardInstrument != null) {
                                instrumentEventSource.dispatch(new InstrumentEvent(InstrumentEvent.INSTRUMENT_LOAD, keyboardInstrument));
                            }
                        }
                    }));
        }

        return project;
    }

    public Instrument getKeyboardInstrument() {
        return keyboardInstrument;
    }

    public void setKeyboardInstrument(Instrument keyboardInstrument) {
        this.keyboardInstrument = keyboardInstrument;
    }

    public Pattern getPianoRollPattern() {
        return pianoRollPattern;
    }

    public void setPianoRollPattern(Pattern pianoRollPattern) {
        this.pianoRollPattern = pianoRollPattern;
    }

    public Instrument getCreateDialogInstrument() {
        return createDialogInstrument;
    }

    public void setCreateDialogInstrument(Instrument createDialogInstrument) {
        this.createDialogInstrument = createDialogInstrument;
    }

    public Instrument getEditDialogInstrument() {
        return editDialogInstrument;
    }

    public void setEditDialogInstrument(Instrument editDialogInstrument) {
        this.editDialogInstrument = editDialogInstrument;
    }

    public MidiEventDispatcher getMidiEventDispatcher() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            MidiManager midiManager = (MidiManager) getApplication().getSystemService(Context.MIDI_SERVICE);
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
        Map<Long, VisualNote> noteViews = new HashMap<>();
        for(ScheduledNoteEvent event : pattern.getEvents()) {
            if(event.action == NoteEvent.NOTE_ON) {
                VisualNote v = new VisualNote(event);
                noteViews.put(event.noteId, v);
            } else if(event.action == NoteEvent.NOTE_OFF) {
                VisualNote v = noteViews.get(event.noteId);
                if(v != null) {
                    v.eventOff = event;
                    v.calculateParams();

                    List<VisualNote> viewsForInstrument = derivedMap.get(event.instrument);
                    if(viewsForInstrument == null) {
                        viewsForInstrument = new ArrayList<>();
                        derivedMap.put(event.instrument, viewsForInstrument);
                    }
                    viewsForInstrument.add(v);
                }
            }
        }

        derivedData = new PatternDerivedData(derivedMap);
        return derivedData;
    }
}
