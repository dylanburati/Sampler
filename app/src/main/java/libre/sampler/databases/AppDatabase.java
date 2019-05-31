package libre.sampler.databases;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import libre.sampler.models.Instrument;
import libre.sampler.models.Pattern;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;
import libre.sampler.models.ScheduledNoteEvent;

@Database(entities = {Project.class, Instrument.class, Sample.class, Pattern.class, ScheduledNoteEvent.class}, version = 6)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProjectDao projectDao();
    public abstract InstrumentDao instrumentDao();
    public abstract SampleDao sampleDao();
    public abstract PatternDao patternDao();
    public abstract ScheduledNoteEventDao scheduledNoteEventDao();

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

    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pattern` (" +
                    "`projectId` INTEGER NOT NULL," +
                    "`id` INTEGER NOT NULL," +
                    "`name` TEXT," +
                    "`nanosPerTick` REAL NOT NULL," +
                    "`loopLengthTicks` INTEGER NOT NULL," +
                    "PRIMARY KEY(`projectId`, `id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `scheduledNoteEvent` (" +
                    "`id` INTEGER NOT NULL," +
                    "`patternId` INTEGER NOT NULL," +
                    "`offsetTicks` INTEGER," +
                    "`action` INTEGER NOT NULL," +
                    "`instrumentId` INTEGER NOT NULL," +
                    "`keyNum` INTEGER NOT NULL," +
                    "`velocity` INTEGER NOT NULL," +
                    "`noteId` INTEGER NOT NULL," +
                    "PRIMARY KEY(`patternId`, `id`))");
        }
    };

    public static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `scheduledNoteEvent` RENAME TO `tmp_scheduledNoteEvent`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `scheduledNoteEvent` (" +
                    "`id` INTEGER NOT NULL," +
                    "`patternId` INTEGER NOT NULL," +
                    "`offsetTicks` INTEGER NOT NULL," +
                    "`action` INTEGER NOT NULL," +
                    "`instrumentId` INTEGER NOT NULL," +
                    "`keyNum` INTEGER NOT NULL," +
                    "`velocity` INTEGER NOT NULL," +
                    "`noteId` INTEGER NOT NULL," +
                    "PRIMARY KEY(`patternId`, `id`))");
            database.execSQL("INSERT INTO `scheduledNoteEvent` SELECT * FROM `tmp_scheduledNoteEvent`");
            database.execSQL("DROP TABLE `tmp_scheduledNoteEvent`");
        }
    };

    public static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `instrument` ADD COLUMN `volume` REAL NOT NULL DEFAULT 1");
            database.execSQL("ALTER TABLE `sample` ADD COLUMN `volume` REAL NOT NULL DEFAULT 1");
        }
    };
}
