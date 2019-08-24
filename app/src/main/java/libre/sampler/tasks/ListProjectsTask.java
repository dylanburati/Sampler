package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.models.Project;
import libre.sampler.utils.DatabaseConnectionManager;

public class ListProjectsTask extends AsyncTask<Void, Void, List<Project>> {
    private Consumer<List<Project>> onCompleted;

    public ListProjectsTask(Consumer<List<Project>> onCompleted) {
        this.onCompleted = onCompleted;
    }

    @Override
    protected List<Project> doInBackground(Void... voids) {
        List<Project> projects = DatabaseConnectionManager.getInstance().projectDao().getAll();
        return projects;
    }

    @Override
    protected void onPostExecute(List<Project> projects) {
        if(this.onCompleted != null) this.onCompleted.accept(projects);
    }
}
