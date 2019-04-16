package libre.sampler.databases;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.models.Sample;

@Database(entities = {Project.class, Instrument.class, Sample.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract ProjectDao projectDao();
    public abstract InstrumentDao instrumentDao();
    public abstract SampleDao sampleDao();
}
