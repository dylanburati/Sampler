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
import libre.sampler.models.Sample;
import libre.sampler.utils.DatabaseConnectionManager;

public class GetInstrumentsTask extends AsyncTask<Void, Void, List<Instrument>> {
    private int projectId;
    private Consumer<List<Instrument>> onCompleted;

    public GetInstrumentsTask(int projectId, Consumer<List<Instrument>> onCompleted) {
        this.projectId = projectId;
        this.onCompleted = onCompleted;
    }

    @Override
    protected List<Instrument> doInBackground(Void... voids) {
        List<Instrument> out = new ArrayList<>();
        List<InstrumentDao.ProjectInstrumentRelation> data = DatabaseConnectionManager.getInstance().instrumentDao().getAll(projectId);
        for(InstrumentDao.ProjectInstrumentRelation d : data) {
            if(d.project.id == projectId) {
                List<Integer> instrumentIds = new ArrayList<>();
                for(Instrument t : d.instruments) {
                    instrumentIds.add(t.id);
                }
                List<SampleDao.InstrumentSampleRelation> sampleData = DatabaseConnectionManager.getInstance().sampleDao().getAll(instrumentIds);
                for(SampleDao.InstrumentSampleRelation r : sampleData) {
                    Collections.sort(r.samples, new Comparator<Sample>() {
                        @Override
                        public int compare(Sample o1, Sample o2) {
                            return Integer.compare(o1.id, o2.id);
                        }
                    });
                    r.instrument.setSamples(r.samples);
                    out.add(r.instrument);
                }
                Collections.sort(out, new Comparator<Instrument>() {
                    @Override
                    public int compare(Instrument o1, Instrument o2) {
                        return Integer.compare(o1.id, o2.id);
                    }
                });
                return out;
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(List<Instrument> instruments) {
        if(this.onCompleted != null) this.onCompleted.accept(instruments);
    }
}
