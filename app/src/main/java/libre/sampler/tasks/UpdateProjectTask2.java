package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.ProjectDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.DatabaseConnectionManager;

public class UpdateProjectTask2 extends AsyncTask<Void, Void, Void> {
    private Project project;
    private Runnable onCompleted;

    /**
     * Does not update the patterns or scheduled note events.
     */
    public UpdateProjectTask2(Project project, Runnable onCompleted) {
        this.project = project;
        project.mtime = System.currentTimeMillis();
        this.onCompleted = onCompleted;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ProjectDao projectDao = DatabaseConnectionManager.getInstance().projectDao();
        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance().instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance().sampleDao();

        projectDao.updateAll(project);
        List<ProjectDao.ProjectWithRelations> data = projectDao.getWithRelations(project.id);
        for(ProjectDao.ProjectWithRelations d : data) {
            if(d.project.id == project.id) {
                List<Integer> instrumentIds = new ArrayList<>();
                for(Instrument t : d.instruments) {
                    instrumentIds.add(t.id);
                }
                sampleDao.deleteAll(instrumentIds);
                instrumentDao.deleteAll(project.id);
            }
        }
        instrumentDao.insertAll(project.getInstruments());
        for(Instrument t : project.getInstruments()) {
            sampleDao.insertAll(t.getSamples());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(this.onCompleted != null) {
            this.onCompleted.run();
        }
    }
}
