package libre.sampler.models;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.publishers.NoteEventSource;
import libre.sampler.publishers.PatternEventSource;
import libre.sampler.publishers.ProjectEventSource;
import libre.sampler.tasks.GetInstrumentsTask;
import libre.sampler.utils.DatabaseConnectionManager;

public class ProjectViewModel extends AndroidViewModel {
    public int projectId = -1;
    // public String projectName = "";

    private Project project;
    private Instrument keyboardInstrument;
    private Pattern pianoRollPattern;
    private Instrument createDialogInstrument;
    private Instrument editDialogInstrument;

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
}
