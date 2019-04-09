package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "project")
public class Project implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "mtime")
    public long mtime;

    @Ignore
    private static final int SAMPLES_PER_INSTRUMENT = 24;
    @Ignore
    public List<Instrument> instruments;

    public Project(int id, String name, Long mtime) {
        this.id = id;
        this.name = name;
        if(mtime != null) this.mtime = mtime;

        this.instruments = new ArrayList<>();
    }

    public String getRelativeTime() {
        StringBuilder relativeTime = new StringBuilder();
        Date then = new Date(mtime);
        Date now = new Date();
        if(then.getYear() == now.getYear()) {
            return (new SimpleDateFormat("MMM d h:mm a", Locale.US)).format(then);
        } else {
            return (new SimpleDateFormat("MMM d, yyyy", Locale.US)).format(then);
        }
    }

    protected Project(Parcel in) {
        id = in.readInt();
        name = in.readString();
        mtime = in.readLong();

        this.instruments = new ArrayList<>();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeLong(mtime);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Project> CREATOR = new Creator<Project>() {
        @Override
        public Project createFromParcel(Parcel in) {
            return new Project(in);
        }

        @Override
        public Project[] newArray(int size) {
            return new Project[size];
        }
    };

    public int addInstrument(Instrument e) {
        int insertIdx = instruments.size();
        instruments.add(insertIdx, e);
        return insertIdx;
    }

    public List<Sample> getSamples(int instrumentIdx, NoteEvent event) {
        Instrument instrument = this.instruments.get(instrumentIdx);
        List<Sample> retval = new ArrayList<>(1);
        for(int i = 0; i < instrument.samples.size(); i++) {
            if(instrument.sampleZones.get(i).contains(event)) {
                retval.add(instrument.samples.get(i));
                return retval;  // todo allow multiple samples per event
            }
        }
        return retval;
    }
}
