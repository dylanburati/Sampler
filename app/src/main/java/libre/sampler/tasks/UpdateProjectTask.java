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

public class UpdateProjectTask extends AsyncTask<Void, Void, Void> {
    private Project project;
    private Runnable onCompleted;

    public UpdateProjectTask(Project project, Runnable onCompleted) {
        this.project = project;
        this.onCompleted = onCompleted;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ProjectDao projectDao = DatabaseConnectionManager.getInstance().projectDao();
        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance().instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance().sampleDao();
        PatternDao patternDao = DatabaseConnectionManager.getInstance().patternDao();
        ScheduledNoteEventDao scheduledNoteEventDao = DatabaseConnectionManager.getInstance().scheduledNoteEventDao();

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

                List<Integer> patternIds = new ArrayList<>();
                for(Pattern p : d.patterns) {
                    patternIds.add(p.id);
                }
                scheduledNoteEventDao.deleteAll(patternIds);
                patternDao.deleteAll(project.id);
            }
        }
        instrumentDao.insertAll(project.getInstruments());
        for(Instrument t : project.getInstruments()) {
            sampleDao.insertAll(t.getSamples());
        }
        patternDao.insertAll(project.getPatterns());
        for(Pattern p : project.getPatterns()) {
            scheduledNoteEventDao.insertAll(p.events);
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(this.onCompleted != null) this.onCompleted.run();
    }
}
