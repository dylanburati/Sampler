package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import libre.sampler.R;
import libre.sampler.databases.ProjectSettingsConverter;
import libre.sampler.publishers.InstrumentEventSource;
import libre.sampler.utils.AppConstants;

@Entity(tableName = "project")
public class Project implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public long mtime;
    private int activeIdx = -1;

    @TypeConverters(ProjectSettingsConverter.class)
    public Map<String, Object> settings;

    @Ignore
    private List<Instrument> instruments;


    @Ignore
    public InstrumentEventSource instrumentEventSource;

    public Project(int id, String name, Long mtime) {
        this.id = id;
        this.name = name;
        if(mtime != null) this.mtime = mtime;
        this.settings = new HashMap<>();
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

    public int addInstrument(Instrument e) {
        e.projectId = this.id;

        boolean uniqueName = checkName(e);
        int suffix = 1;
        String origName = e.name;
        while(!uniqueName) {
            e.name = String.format("%s (%d)", origName, suffix);
            uniqueName = checkName(e);
        }
        int insertIdx = instruments.size();
        e.setInstrumentId(this.id * AppConstants.MAX_INSTRUMENTS_PER_PROJECT + insertIdx);
        instruments.add(insertIdx, e);
        return insertIdx;
    }

    public int removeInstrument(Instrument e) {
        int removeIdx = instruments.indexOf(e);
        if(removeIdx == -1) {
            return -1;
        }
        instruments.remove(removeIdx);
        for(int i = removeIdx; i < instruments.size(); i++) {
            instruments.get(i).setInstrumentId(this.id * AppConstants.MAX_INSTRUMENTS_PER_PROJECT + i);
        }
        return removeIdx;
    }

    public void setInstruments(List<Instrument> instruments) {
        this.instruments = instruments;
    }

    public List<Sample> getSamples(NoteEvent event) {
        return getSamples(getActiveInstrument(), event);
    }

    public List<Sample> getSamples(Instrument instrument, NoteEvent event) {
        if(instrument == null) {
            return Collections.emptyList();
        }
        List<Sample> retval = new ArrayList<>(10);
        for(int i = 0; i < instrument.getSamples().size(); i++) {
            if(instrument.getSamples().get(i).contains(event)) {
                retval.add(instrument.getSamples().get(i));
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

    public int getActiveIdx() {
        if(activeIdx >= this.instruments.size()) {
            return -1;
        }
        return activeIdx;
    }

    public Instrument getActiveInstrument() {
        if(activeIdx < 0 || activeIdx >= this.instruments.size()) {
            return null;
        }
        return this.instruments.get(activeIdx);
    }

    public int setActiveInstrument(Instrument t) {
        int idx = this.instruments.indexOf(t);
        if(idx == -1) {
            idx = addInstrument(t);
        }
        activeIdx = idx;

        instrumentEventSource.dispatch(t);
        return activeIdx;
    }

    public void updateActiveInstrument() {
        instrumentEventSource.dispatch(getActiveInstrument());
    }

    public void setActiveIdx(int activeIdx) {
        this.activeIdx = activeIdx;
        instrumentEventSource.dispatch(getActiveInstrument());
    }

    public List<Instrument> getInstruments() {
        return instruments;
    }

    public String getDefaultSamplePath() {
        String s = (String) settings.get(AppConstants.PREF_DEFAULT_SAMPLE_PATH);
        if(s == null) {
            return "";
        }
        return s;
    }

    public void setDefaultSamplePath(String s) {
        settings.put(AppConstants.PREF_DEFAULT_SAMPLE_PATH, s);
    }

    protected Project(Parcel in) {
        id = in.readInt();
        name = in.readString();
        mtime = in.readLong();
        instruments = in.createTypedArrayList(Instrument.CREATOR);
        activeIdx = in.readInt();
        settings = ProjectSettingsConverter.deserializeSettings(in.readString());

        this.instrumentEventSource = new InstrumentEventSource();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeLong(mtime);
        dest.writeTypedList(instruments);
        dest.writeInt(activeIdx);
        dest.writeString(ProjectSettingsConverter.serializeSettings(settings));
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
}
