package libre.sampler.databases;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;

@Database(entities = {Project.class, Instrument.class, Sample.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProjectDao projectDao();
    public abstract InstrumentDao instrumentDao();
    public abstract SampleDao sampleDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `project` ADD COLUMN `settings` TEXT");
            database.execSQL("UPDATE `project` SET `settings` = \"{}\"");
        }
    };
}
