package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.PatternDao;
import libre.sampler.databases.ProjectDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.databases.ScheduledNoteEventDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Pattern;
import libre.sampler.models.Project;
import libre.sampler.utils.DatabaseConnectionManager;

public class DeleteProjectsTask extends AsyncTask<Void, Void, Void> {
    private final Runnable postHook;
    private List<Project> projects;

    public DeleteProjectsTask(List<Project> projects, Runnable postHook) {
        this.projects = projects;
        this.postHook = postHook;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ProjectDao projectDao = DatabaseConnectionManager.getInstance().projectDao();
        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance().instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance().sampleDao();
        PatternDao patternDao = DatabaseConnectionManager.getInstance().patternDao();
        ScheduledNoteEventDao scheduledNoteEventDao = DatabaseConnectionManager.getInstance().scheduledNoteEventDao();

        List<Integer> projectIds = new ArrayList<>();
        for(Project p : projects) {
            projectIds.add(p.id);
        }
        List<ProjectDao.ProjectWithRelations> data = projectDao.getWithRelations(projectIds);
        List<Integer> instrumentIds = new ArrayList<>();
        List<Integer> patternIds = new ArrayList<>();
        for(ProjectDao.ProjectWithRelations prj : data) {
            for(Instrument t : prj.instruments) {
                instrumentIds.add(t.id);
            }
            for(Pattern p : prj.patterns) {
                patternIds.add(p.id);
            }
        }

        sampleDao.deleteAll(instrumentIds);
        scheduledNoteEventDao.deleteAll(patternIds);
        instrumentDao.deleteAll(projectIds);
        patternDao.deleteAll(projectIds);
        projectDao.deleteAll(projectIds);
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        postHook.run();
    }
}
