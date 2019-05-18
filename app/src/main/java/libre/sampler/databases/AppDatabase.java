package libre.sampler.databases;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;

@Database(entities = {Project.class, Instrument.class, Sample.class}, version = 3)
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

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `project` RENAME TO `tmp_project`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `project` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                    "`name` TEXT," +
                    "`mtime` INTEGER NOT NULL," +
                    "`settings` TEXT)");
            database.execSQL("INSERT INTO `project` SELECT `id`, `name`, `mtime`, `settings` FROM `tmp_project`");
            database.execSQL("DROP TABLE `tmp_project`");
        }
    };
}
