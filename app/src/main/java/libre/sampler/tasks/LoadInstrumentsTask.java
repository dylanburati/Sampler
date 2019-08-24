package libre.sampler.tasks;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.core.util.Consumer;
import libre.sampler.databases.InstrumentDao;
import libre.sampler.models.Instrument;
import libre.sampler.models.Sample;
import libre.sampler.utils.DatabaseConnectionManager;

public class LoadInstrumentsTask extends AsyncTask<Void, Void, List<Instrument>> {
    private Consumer<List<Instrument>> instrumentCallback;

    public LoadInstrumentsTask(Consumer<List<Instrument>> instrumentCallback) {
        this.instrumentCallback = instrumentCallback;
    }

    @Override
    protected List<Instrument> doInBackground(Void... voids) {
        List<Instrument> allInstruments = new ArrayList<>();

        List<InstrumentDao.InstrumentWithRelations> sampleData = DatabaseConnectionManager.getInstance().instrumentDao().getAllWithRelations();
        for(InstrumentDao.InstrumentWithRelations r : sampleData) {
            Collections.sort(r.samples, new Comparator<Sample>() {
                @Override
                public int compare(Sample o1, Sample o2) {
                    return Integer.compare(o1.id, o2.id);
                }
            });
            r.instrument.setSamples(r.samples);
            allInstruments.add(r.instrument);
        }
        return allInstruments;
    }

    @Override
    protected void onPostExecute(List<Instrument> data) {
        if(this.instrumentCallback != null) this.instrumentCallback.accept(data);
    }
}
