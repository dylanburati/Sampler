package libre.sampler.tasks;

import android.os.AsyncTask;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import libre.sampler.databases.InstrumentDao;
import libre.sampler.databases.SampleDao;
import libre.sampler.io.ProjectDeserializer;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.DatabaseConnectionManager;
import libre.sampler.utils.ProgressFraction;

public class ImportProjectTask extends AsyncTask<Void, Float, String> implements ProgressFraction {
    private Project project;
    private String url;
    private File extractDirectory;
    private final Callbacks callbackObj;

    public ImportProjectTask(Project project, String url, File extractDirectory, Callbacks callbackObj) {
        this.project = project;
        this.url = url;
        this.extractDirectory = extractDirectory;
        this.callbackObj = callbackObj;
    }

    @Override
    protected String doInBackground(Void... voids) {
        ProjectDeserializer deserializer = new ProjectDeserializer(project, extractDirectory, this,
                new Runnable() {
                    @Override
                    public void run() {
                        // Only insert project when deserializer has reached the `instruments` array
                        DatabaseConnectionManager.getInstance().projectDao().insert(project);
                    }
                });
        try {
            URL toImport = new URL(this.url);
            URLConnection connection = toImport.openConnection();
            connection.setConnectTimeout(4000);
            InputStream inputStream = new BufferedInputStream(connection.getInputStream());
            deserializer.read(inputStream);
            inputStream.close();
        } catch(IOException e) {
            if(e instanceof java.net.UnknownHostException) {
                return AppConstants.ERROR_COULD_NOT_CONNECT;
            } else if(e instanceof java.net.MalformedURLException) {
                return AppConstants.ERROR_INVALID_URL;
            }
            e.printStackTrace();
            return e.getMessage();
        }

        InstrumentDao instrumentDao = DatabaseConnectionManager.getInstance().instrumentDao();
        SampleDao sampleDao = DatabaseConnectionManager.getInstance().sampleDao();

        instrumentDao.insertAll(project.getInstruments());
        for(Instrument t : project.getInstruments()) {
            sampleDao.insertAll(t.getSamples());
        }

        return AppConstants.SUCCESS_IMPORT_PROJECT;
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
    private float total;

    @Override
    public void setProgressTotal(float total) {
        this.total = total;
    }

    @Override
    public void setProgressCurrent(float current) {
        if(this.total != 0) publishProgress( current / total);
    }
}
