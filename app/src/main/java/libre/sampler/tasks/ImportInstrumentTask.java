package libre.sampler.tasks;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import androidx.core.util.Consumer;
import libre.sampler.io.InstrumentDeserializer;
import libre.sampler.models.Instrument;
import libre.sampler.utils.AppConstants;

public class ImportInstrumentTask extends AsyncTask<Void, Void, String> {
    private Instrument instrument;
    private File inFile;
    private File extractDirectory;
    private final Consumer<String> callback;

    public ImportInstrumentTask(Instrument instrument, File inFile, File extractDirectory,
                                Consumer<String> callback) {
        this.instrument = instrument;
        this.inFile = inFile;
        this.extractDirectory = extractDirectory;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try(InstrumentDeserializer deserializer = new InstrumentDeserializer(instrument)) {
            deserializer.read(inFile, extractDirectory);
        } catch(IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return AppConstants.SUCCESS_IMPORT_INSTRUMENT;
    }

    @Override
    protected void onPostExecute(String s) {
        if(this.callback != null) callback.accept(s);
    }
}
