package libre.sampler.models;

import android.app.Application;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import libre.sampler.publishers.EmptyEventSource;
import libre.sampler.publishers.ProjectEventSource;
import libre.sampler.tasks.CreateProjectTask;
import libre.sampler.tasks.DeleteProjectsTask;
import libre.sampler.tasks.ListProjectsTask;
import libre.sampler.tasks.LoadInstrumentsTask;
import libre.sampler.tasks.UpdateProjectTask2;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.ModelState;

public class MainViewModel extends AndroidViewModel {
    private List<Project> projects;
    private ModelState projectsState = ModelState.INVALID;

    private List<Instrument> allInstruments;
    private ModelState allInstrumentsState = ModelState.INVALID;

    private Project dialogProject;
    private int dialogActionType;

    public final EmptyEventSource loadEventSource = new EmptyEventSource();
    public final ProjectEventSource projectEventSource = new ProjectEventSource();

    public MainViewModel(@NonNull Application application) {
        super(application);
        DatabaseConnectionManager.initialize(application);
    }

    public List<Project> getProjects() {
        if(projectsState == ModelState.INVALID) {
            projectsState = ModelState.LOADING;
            DatabaseConnectionManager.runTask(new ListProjectsTask(new Consumer<List<Project>>() {
                @Override
                public void accept(List<Project> prjs) {
                    MainViewModel.this.projects = prjs;
                    loadEventSource.dispatch(AppConstants.PROJECTS_LOADED);
                    projectsState = ModelState.LOADED;
                }
            }));
        }

        return projects;
    }

    public void invalidateProjects() {
        this.projects = null;
        this.projectsState = ModelState.INVALID;
    }

    public List<Instrument> getInstruments() {
        if(allInstrumentsState == ModelState.INVALID) {
            allInstrumentsState = ModelState.LOADING;
            DatabaseConnectionManager.runTask(new LoadInstrumentsTask(new Consumer<List<Instrument>>() {
                @Override
                public void accept(List<Instrument> instruments) {
                    MainViewModel.this.allInstruments = instruments;
                    loadEventSource.dispatch(AppConstants.ALL_INSTRUMENTS_LOADED);
                    allInstrumentsState = ModelState.LOADED;
                }
            }));
        }

        return allInstruments;
    }

    public void invalidateInstruments() {
        this.allInstruments = null;
        this.allInstrumentsState = ModelState.INVALID;
    }

    public Project getDialogProject() {
        return dialogProject;
    }

    public void setDialogProject(Project dialogProject) {
        this.dialogProject = dialogProject;
    }

    public int getDialogActionType() {
        return dialogActionType;
    }

    public void setDialogActionType(int dialogActionType) {
        this.dialogActionType = dialogActionType;
    }

    public void addNewProject(final Project toAdd, final Collection<Instrument> instruments) {
        if(this.projectsState == ModelState.LOADED) {
            DatabaseConnectionManager.runTask(new CreateProjectTask(toAdd, instruments, new Runnable() {
                @Override
                public void run() {
                    MainViewModel.this.projects.add(0, toAdd);
                    if(MainViewModel.this.allInstrumentsState == ModelState.LOADED) {
                        MainViewModel.this.allInstruments.addAll(toAdd.getInstruments());
                    }
                    projectEventSource.dispatch(new ProjectEvent(ProjectEvent.PROJECT_CREATE, toAdd));
                }
            }));
        }
    }

    public void addImportedProject(final Project toAdd) {
        MainViewModel.this.projects.add(0, toAdd);
        if(MainViewModel.this.allInstrumentsState == ModelState.LOADED) {
            MainViewModel.this.allInstruments.addAll(toAdd.getInstruments());
        }
        projectEventSource.dispatch(new ProjectEvent(ProjectEvent.PROJECT_CREATE, toAdd));
    }

    public void removeProject(final Project project) {
        if(this.projectsState == ModelState.LOADED) {
            this.projects.remove(project);
            DatabaseConnectionManager.runTask(new DeleteProjectsTask(Collections.singletonList(project), new Runnable() {
                @Override
                public void run() {
                    projectEventSource.dispatch(new ProjectEvent(ProjectEvent.PROJECT_DELETE, project));
                }
            }));
        }
    }

    public void updateProject(final Project project, final Collection<Instrument> instruments) {
        if(this.projectsState == ModelState.LOADED && this.allInstrumentsState == ModelState.LOADED) {
            List<Instrument> dbInstruments = new ArrayList<>();
            for(Instrument t : allInstruments) {
                if(t.projectId == project.id) {
                    dbInstruments.add(t);
                }
            }
            project.setInstruments(dbInstruments);
            for(Instrument t : instruments) {
                Instrument tCopy = new Instrument(t.name);
                project.registerInstrument(tCopy);
                tCopy.setVolume(t.getVolume());
                for(Sample s : t.getSamples()) {
                    tCopy.addSample(new Sample(s));
                }
                project.addInstrument(tCopy);
                this.allInstruments.add(tCopy);
            }
            DatabaseConnectionManager.runTask(new UpdateProjectTask2(project, new Runnable() {
                @Override
                public void run() {
                    projectEventSource.dispatch(new ProjectEvent(ProjectEvent.PROJECT_EDIT, project));
                }
            }));
        }
    }
}
