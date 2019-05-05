package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.utils.DatabaseConnectionManager;

public class CreateProjectTask extends AsyncTask<Void, Void, Integer> {
    private Project project;
    private Consumer<Integer> onCompleted;

    public CreateProjectTask(Project project, Consumer<Integer> onCompleted) {
        this.project = project;
        this.onCompleted = onCompleted;
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        return ((int) DatabaseConnectionManager.getInstance().projectDao().insert(project));
    }

    @Override
    protected void onPostExecute(Integer id) {
        if(this.onCompleted != null) this.onCompleted.accept(id);
    }
}
