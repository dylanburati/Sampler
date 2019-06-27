package libre.sampler.tasks;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import androidx.core.util.Consumer;
import libre.sampler.io.InstrumentSerializer;
import libre.sampler.models.Instrument;
import libre.sampler.utils.AppConstants;

public class ExportInstrumentTask extends AsyncTask<Void, Void, String> {
    private Instrument instrument;
    private File outFile;
    private final Consumer<String> callback;

    public ExportInstrumentTask(Instrument instrument, File outFile, Consumer<String> callback) {
        this.instrument = instrument;
        this.outFile = outFile;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try(InstrumentSerializer serializer = new InstrumentSerializer(instrument)) {
            serializer.write(outFile, false);
        } catch(IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return AppConstants.SUCCESS_EXPORT_INSTRUMENT;
    }

    @Override
    protected void onPostExecute(String s) {
        if(this.callback != null) callback.accept(s);
    }
}
