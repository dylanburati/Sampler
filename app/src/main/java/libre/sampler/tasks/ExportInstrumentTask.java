package libre.sampler.tasks;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import libre.sampler.io.InstrumentSerializer;
import libre.sampler.models.Instrument;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.ProgressFraction;

public class ExportInstrumentTask extends AsyncTask<Void, Float, String> implements ProgressFraction {
    private Instrument instrument;
    private File outFile;
    private final Callbacks callbackObj;

    public ExportInstrumentTask(Instrument instrument, File outFile,
                                Callbacks callbackObj) {
        this.instrument = instrument;
        this.outFile = outFile;
        this.callbackObj = callbackObj;
    }

    @Override
    protected String doInBackground(Void... voids) {
        try(InstrumentSerializer serializer = new InstrumentSerializer(instrument, this)) {
            serializer.write(outFile, false);
        } catch(IOException e) {
            e.printStackTrace();
            return e.getMessage();
        }

        return AppConstants.SUCCESS_EXPORT_INSTRUMENT;
    }

    @Override
    protected void onProgressUpdate(Float... values) {
        if(this.callbackObj != null) callbackObj.onProgressUpdate(values[0]);
    }

    @Override
    protected void onPostExecute(String s) {
        if(this.callbackObj != null) callbackObj.onPostExecute(s);
    }

    public interface Callbacks {
        void onProgressUpdate(float progress);
        void onPostExecute(String message);
    }

    // ProgressFraction
    private int total;

    @Override
    public void setProgressTotal(int total) {
        this.total = total;
    }

    @Override
    public void setProgressCurrent(int current) {
        onProgressUpdate((float) current / total);
    }
}
