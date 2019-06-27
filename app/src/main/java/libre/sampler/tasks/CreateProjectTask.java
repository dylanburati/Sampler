package libre.sampler.tasks;

import android.os.AsyncTask;

import libre.sampler.models.Project;
import libre.sampler.utils.DatabaseConnectionManager;

public class CreateProjectTask extends AsyncTask<Void, Void, Integer> {
    private Project project;

    public CreateProjectTask(Project project) {
        this.project = project;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        return ((int) DatabaseConnectionManager.getInstance().projectDao().insert(project));
    }

    @Override
    protected void onPostExecute(Integer id) {
        this.project.setProjectId(id);
    }
}
