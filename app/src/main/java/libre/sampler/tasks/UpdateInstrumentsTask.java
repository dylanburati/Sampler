package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.List;

import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.DatabaseConnectionManager;

public class UpdateInstrumentsTask extends AsyncTask<Void, Void, Void> {
    private Project project;

    public UpdateInstrumentsTask(Project project) {
        this.project = project;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance(null).instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance(null).sampleDao();
        List<InstrumentDao.ProjectInstrumentRelation> data = instrumentDao.getAll(project.id);
        for(InstrumentDao.ProjectInstrumentRelation d : data) {
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
}
