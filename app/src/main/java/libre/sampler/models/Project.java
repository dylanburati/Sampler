package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import libre.sampler.R;
import libre.sampler.publishers.InstrumentEventSource;

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
    @Ignore
    private int activeIdx = -1;
    @Ignore
    public InstrumentEventSource instrumentEventSource;

    public Project(int id, String name, Long mtime) {
        this.id = id;
        this.name = name;
        if(mtime != null) this.mtime = mtime;

        this.instruments = new ArrayList<>();
        this.instrumentEventSource = new InstrumentEventSource();
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
        this.instrumentEventSource = new InstrumentEventSource();
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
        boolean uniqueName = checkName(e);
        int suffix = 1;
        String origName = e.name;
        while(!uniqueName) {
            e.name = String.format("%s (%d)", origName, suffix);
            uniqueName = checkName(e);
        }
        int insertIdx = instruments.size();
        instruments.add(insertIdx, e);
        return insertIdx;
    }

    public List<Sample> getSamples(NoteEvent event) {
        return getSamples(getActiveInstrument(), event);
    }

    public List<Sample> getSamples(Instrument instrument, NoteEvent event) {
        if(instrument == null) {
            return Collections.emptyList();
        }
        List<Sample> retval = new ArrayList<>(10);
        for(int i = 0; i < instrument.samples.size(); i++) {
            if(instrument.samples.get(i).contains(event)) {
                retval.add(instrument.samples.get(i));
                // return retval;  // omitted to allow multiple samples per event
            }
        }
        return retval;
    }

    public boolean checkName(Instrument e) {
        if(e.name == null || e.name.isEmpty()) {
            e.name = "New instrument";
        }
        for(Instrument t : instruments) {
            if(e.name.equals(t.name)) {
                return false;
            }
        }
        return true;
    }

    public Instrument getActiveInstrument() {
        if(activeIdx < 0 || activeIdx >= this.instruments.size()) {
            return null;
        }
        return this.instruments.get(activeIdx);
    }

    public void setActiveInstrument(Instrument t) {
        int idx = this.instruments.indexOf(t);
        if(idx == -1) {
            idx = addInstrument(t);
        }
        activeIdx = idx;

        instrumentEventSource.dispatch(t);
    }
}
