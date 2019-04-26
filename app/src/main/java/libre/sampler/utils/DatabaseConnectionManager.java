package libre.sampler.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.Room;
import libre.sampler.databases.AppDatabase;

public class DatabaseConnectionManager {
    private static AppDatabase instance = null;
    private static ExecutorService executor = Executors.newSingleThreadExecutor();
    public static boolean ready = false;

    public static AppDatabase getInstance(Context ctx) {
        if(instance == null) {
            if(ctx == null) {
                throw new NullPointerException("A valid context must be provided to initialize the database");
            }
            instance = Room.databaseBuilder(ctx, AppDatabase.class, "sampler").addMigrations(AppDatabase.MIGRATION_1_2).build();
        }
        ready = true;
        return instance;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }

    public static void runTask(AsyncTask<Void, Void, ?> task) {
        task.executeOnExecutor(executor);
    }
}
