package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.Collection;

import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.utils.DatabaseConnectionManager;

public class CreateProjectTask extends AsyncTask<Void, Void, Void> {
    private Project project;
    private Collection<Instrument> instruments;
    private Runnable onCompleted;

    public CreateProjectTask(Project project, Collection<Instrument> instruments, Runnable onCompleted) {
        this.project = project;
        this.instruments = instruments;
        this.onCompleted = onCompleted;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance().instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance().sampleDao();

        int projectId = (int) DatabaseConnectionManager.getInstance().projectDao().insert(project);
        this.project.setProjectId(projectId);
        for(Instrument t : instruments) {
            Instrument tCopy = new Instrument(t.name);
            this.project.registerInstrument(tCopy);
            tCopy.setVolume(t.getVolume());
            for(Sample s : t.getSamples()) {
                tCopy.addSample(new Sample(s));
            }
            this.project.addInstrument(tCopy);
        }

        instrumentDao.insertAll(project.getInstruments());
        for(Instrument t : project.getInstruments()) {
            sampleDao.insertAll(t.getSamples());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        if(this.onCompleted != null) this.onCompleted.run();
    }
}
