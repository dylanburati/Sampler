package libre.sampler.models;

import android.app.Application;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.lifecycle.AndroidViewModel;
import libre.sampler.publishers.EmptyEventSource;
import libre.sampler.publishers.ProjectEventSource;
import libre.sampler.tasks.GetProjectsTask;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;

public class MainViewModel extends AndroidViewModel {
    private List<Project> projects;
    private Project dialogProject;

    public final EmptyEventSource loadEventSource = new EmptyEventSource();
    public final ProjectEventSource projectEventSource = new ProjectEventSource();

    public MainViewModel(@NonNull Application application) {
        super(application);
    }

    public List<Project> getProjects() {
        if(projects == null) {
            DatabaseConnectionManager.initialize(getApplication());
            DatabaseConnectionManager.runTask(new GetProjectsTask(new Consumer<List<Project>>() {
                @Override
                public void accept(List<Project> prjs) {
                    MainViewModel.this.projects = prjs;
                    loadEventSource.dispatch(AppConstants.PROJECTS_LOADED);
                }
            }));
        }

        return projects;
    }

    public Project getDialogProject() {
        return dialogProject;
    }

    public void setDialogProject(Project dialogProject) {
        this.dialogProject = dialogProject;
    }
}
