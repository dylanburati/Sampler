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

@Database(entities = {Project.class, Instrument.class, Sample.class, Pattern.class, ScheduledNoteEvent.class}, version = 10)
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

    public static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `sample` RENAME TO `tmp_sample`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sample` (" +
                    "`instrumentId` INTEGER NOT NULL," +
                    "`id` INTEGER NOT NULL," +
                    "`filename` TEXT," +
                    "`volume` REAL NOT NULL," +
                    "`minPitch` INTEGER NOT NULL," +
                    "`maxPitch` INTEGER NOT NULL," +
                    "`minVelocity` INTEGER NOT NULL," +
                    "`maxVelocity` INTEGER NOT NULL," +
                    "`attack` REAL NOT NULL," +
                    "`decay` REAL NOT NULL," +
                    "`sustain` REAL NOT NULL," +
                    "`release` REAL NOT NULL," +
                    "`basePitch` INTEGER NOT NULL," +
                    "`startTime` REAL NOT NULL," +
                    "`resumeTime` REAL NOT NULL," +
                    "`endTime` REAL NOT NULL," +
                    "`displayFlags` INTEGER NOT NULL," +
                    "PRIMARY KEY(`instrumentId`, `id`))"
            );
            database.execSQL("INSERT INTO `sample` SELECT " +
                    "`instrumentId`, `id`, `filename`, `volume`, `minPitch`, `maxPitch`," +
                    "`minVelocity`, `maxVelocity`, `attack`, `decay`, `sustain`, `release`," +
                    "`basePitch`, `startTime`, `resumeTime`, `endTime`, `displayFlags` " +
                    "FROM `tmp_sample`"
            );
            database.execSQL("DROP TABLE `tmp_sample`");
        }
    };

    public static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `sample` RENAME TO `tmp_sample`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sample` (" +
                    "`instrumentId` INTEGER NOT NULL," +
                    "`id` INTEGER NOT NULL," +
                    "`filename` TEXT," +
                    "`volume` REAL NOT NULL," +
                    "`minPitch` INTEGER NOT NULL," +
                    "`maxPitch` INTEGER NOT NULL," +
                    "`minVelocity` INTEGER NOT NULL," +
                    "`maxVelocity` INTEGER NOT NULL," +
                    "`attack` REAL NOT NULL," +
                    "`decay` REAL NOT NULL," +
                    "`sustain` REAL NOT NULL," +
                    "`release` REAL NOT NULL," +
                    "`basePitch` REAL NOT NULL," +
                    "`startTime` REAL NOT NULL," +
                    "`resumeTime` REAL NOT NULL," +
                    "`endTime` REAL NOT NULL," +
                    "`displayFlags` INTEGER NOT NULL," +
                    "PRIMARY KEY(`instrumentId`, `id`))"
            );
            database.execSQL("INSERT INTO `sample` SELECT " +
                    "`instrumentId`, `id`, `filename`, `volume`, `minPitch`, `maxPitch`," +
                    "`minVelocity`, `maxVelocity`, `attack`, `decay`, `sustain`, `release`," +
                    "`basePitch`, `startTime`, `resumeTime`, `endTime`, `displayFlags` " +
                    "FROM `tmp_sample`"
            );
            database.execSQL("DROP TABLE `tmp_sample`");
        }
    };

    public static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `scheduledNoteEvent` RENAME TO `tmp_scheduledNoteEvent`");
            database.execSQL("ALTER TABLE `pattern` RENAME TO `tmp_pattern`");
            database.execSQL("ALTER TABLE `sample` RENAME TO `tmp_sample`");
            database.execSQL("ALTER TABLE `instrument` RENAME TO `tmp_instrument`");
            database.execSQL("ALTER TABLE `project` RENAME TO `tmp_project`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `project` (`id` TEXT NOT NULL, `name` TEXT, `mtime` INTEGER NOT NULL, `settings` TEXT, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `instrument` (`projectId` TEXT, `id` TEXT NOT NULL, `name` TEXT, `volume` REAL NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sample` (`instrumentId` TEXT, `id` TEXT NOT NULL, `filename` TEXT, `volume` REAL NOT NULL, `minPitch` INTEGER NOT NULL, `maxPitch` INTEGER NOT NULL, `minVelocity` INTEGER NOT NULL, `maxVelocity` INTEGER NOT NULL, `attack` REAL NOT NULL, `decay` REAL NOT NULL, `sustain` REAL NOT NULL, `release` REAL NOT NULL, `basePitch` REAL NOT NULL, `startTime` REAL NOT NULL, `resumeTime` REAL NOT NULL, `endTime` REAL NOT NULL, `displayFlags` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `pattern` (`id` TEXT NOT NULL, `projectId` TEXT, `name` TEXT, `nanosPerTick` REAL NOT NULL, `loopLengthTicks` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `scheduledNoteEvent` (`patternId` TEXT, `id` TEXT NOT NULL, `offsetTicks` INTEGER NOT NULL, `action` INTEGER NOT NULL, `instrumentId` TEXT, `keyNum` INTEGER NOT NULL, `velocity` INTEGER NOT NULL, `noteId` INTEGER NOT NULL, PRIMARY KEY(`id`))");

            database.execSQL("INSERT INTO `project` SELECT `id`, `name`, `mtime`, `settings` FROM `tmp_project`");
            database.execSQL("INSERT INTO `instrument` SELECT `projectId`, `id`, `name`, `volume` FROM `tmp_instrument`");
            database.execSQL("INSERT INTO `sample` SELECT `instrumentId`, `id`, `filename`, `volume`, `minPitch`, `maxPitch`, `minVelocity`, `maxVelocity`, `attack`, `decay`, `sustain`, `release`, `basePitch`, `startTime`, `resumeTime`, `endTime`, `displayFlags` FROM `tmp_sample`");
            database.execSQL("INSERT INTO `pattern` SELECT `id`, `projectId`, `name`, `nanosPerTick`, `loopLengthTicks` FROM `tmp_pattern`");
            database.execSQL("INSERT INTO `scheduledNoteEvent` SELECT `patternId`, `id`, `offsetTicks`, `action`, `instrumentId`, `keyNum`, `velocity`, `noteId` FROM `tmp_scheduledNoteEvent`");

            database.execSQL("DROP TABLE `tmp_scheduledNoteEvent`");
            database.execSQL("DROP TABLE `tmp_pattern`");
            database.execSQL("DROP TABLE `tmp_sample`");
            database.execSQL("DROP TABLE `tmp_instrument`");
            database.execSQL("DROP TABLE `tmp_project`");
        }
    };

    public static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `instrument` RENAME TO `tmp_instrument`");
            database.execSQL("ALTER TABLE `sample` RENAME TO `tmp_sample`");
            database.execSQL("ALTER TABLE `pattern` RENAME TO `tmp_pattern`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `instrument` (`projectId` TEXT, `id` TEXT NOT NULL, `name` TEXT, `sort` INTEGER NOT NULL, `volume` REAL NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sample` (`instrumentId` TEXT, `id` TEXT NOT NULL, `filename` TEXT, `sort` INTEGER NOT NULL, `volume` REAL NOT NULL, `minPitch` INTEGER NOT NULL, `maxPitch` INTEGER NOT NULL, `minVelocity` INTEGER NOT NULL, `maxVelocity` INTEGER NOT NULL, `attack` REAL NOT NULL, `decay` REAL NOT NULL, `sustain` REAL NOT NULL, `release` REAL NOT NULL, `basePitch` REAL NOT NULL, `startTime` REAL NOT NULL, `resumeTime` REAL NOT NULL, `endTime` REAL NOT NULL, `displayFlags` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `pattern` (`id` TEXT NOT NULL, `projectId` TEXT, `name` TEXT, `sort` INTEGER NOT NULL, `nanosPerTick` REAL NOT NULL, `loopLengthTicks` INTEGER NOT NULL, PRIMARY KEY(`id`))");

            database.execSQL("INSERT INTO `instrument` SELECT `projectId`, `id`, `name`, 0 AS `sort`, `volume` FROM `tmp_instrument`");
            database.execSQL("INSERT INTO `sample` SELECT `instrumentId`, `id`, `filename`, 0 AS `sort`, `volume`, `minPitch`, `maxPitch`, `minVelocity`, `maxVelocity`, `attack`, `decay`, `sustain`, `release`, `basePitch`, `startTime`, `resumeTime`, `endTime`, `displayFlags` FROM `tmp_sample`");
            database.execSQL("INSERT INTO `pattern` SELECT `id`, `projectId`, `name`, 0 AS `sort`, `nanosPerTick`, `loopLengthTicks` FROM `tmp_pattern`");

            database.execSQL("DROP TABLE `tmp_instrument`");
            database.execSQL("DROP TABLE `tmp_sample`");
            database.execSQL("DROP TABLE `tmp_pattern`");
        }
    };
}
