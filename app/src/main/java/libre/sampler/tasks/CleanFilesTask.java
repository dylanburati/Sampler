package libre.sampler.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import libre.sampler.models.Sample;
import libre.sampler.utils.DatabaseConnectionManager;

public class CleanFilesTask extends AsyncTask<Void, Void, String> {
    private File dataDir;
    private WeakReference<Context> contextRef;

    public CleanFilesTask(File dataDir, Context context) {
        this.dataDir = dataDir;
        this.contextRef = new WeakReference<>(context);
    }

    @Override
    protected String doInBackground(Void... voids) {
        List<Sample> samples = DatabaseConnectionManager.getInstance().sampleDao().selectAll();
        Set<String> sampleFiles = new TreeSet<>();
        for(Sample s : samples) {
            sampleFiles.add(s.filename);
        }
        int numDeleted = 0;

        // List<Project> projects = DatabaseConnectionManager.getInstance().projectDao().getAll();
        Set<File> sampleDirs = new HashSet<>();
        sampleDirs.add(dataDir);
        // for(Project p : projects) {
        //     String path = p.getDefaultSamplePath();
        //     if(!path.isEmpty()) {
        //         File dir = new File(path);
        //         if(dir.isDirectory() && dir.canWrite()) {
        //             sampleDirs.add(dir);
        //         }
        //     }
        // }
        for(File dir : sampleDirs) {
            for(File file : dir.listFiles()) {
                if(!file.isFile() || !file.canWrite()) {
                    continue;
                }
                String filename = file.getAbsolutePath();
                if(!sampleFiles.contains(filename)) {
                    // Log.d("CleanFilesTask", filename);
                    numDeleted += (file.delete() ? 1 : 0);
                }
            }
        }

        return String.format("Deleted %d files", numDeleted);
    }

    @Override
    protected void onPostExecute(String s) {
        Context context = contextRef.get();
        if(context == null) {
            return;
        }
        Toast.makeText(context, s, Toast.LENGTH_SHORT).show();
    }
}
