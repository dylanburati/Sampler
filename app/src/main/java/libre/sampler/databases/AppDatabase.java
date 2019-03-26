package libre.sampler.databases;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import libre.sampler.models.Project;

@Database(entities = {Project.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProjectDao projectDao();
}
