package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "instrument", primaryKeys = {"projectId", "id"})
public class Instrument implements Parcelable {
    public int projectId;
    public int id;
    public String name;

    @Ignore
    private List<Sample> samples = new ArrayList<>();
    @Ignore
    private List<String> filenames = new ArrayList<>();

    public Instrument(String name) {
        this.name = name;
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
        int nextSampleIdx = 0;
        for(Sample s : samples) {
            s.instrumentId = this.id;
            int sampleIdx = this.filenames.indexOf(s.filename);
            if(sampleIdx == -1) {
                s.sampleIndex = nextSampleIdx++;
                this.filenames.add(s.filename);
            } else {
                s.sampleIndex = sampleIdx;
            }
        }
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public void setInstrumentId(int id) {
        this.id = id;
        for(Sample s : this.samples) {
            s.instrumentId = id;
        }
    }

    public Sample addSample(String filename) {
        int insertIdx = this.samples.size();
        int sampleIdx = this.filenames.indexOf(filename);
        if(sampleIdx == -1) {
            sampleIdx = this.filenames.size();
            this.filenames.add(filename);
        }

        Sample s = new Sample(filename, sampleIdx, insertIdx);
        s.instrumentId = this.id;
        samples.add(s);
        return s;
    }

    public void addSample(Sample s) {
        int insertIdx = this.samples.size();
        int sampleIdx = this.filenames.indexOf(s.filename);
        if(sampleIdx == -1) {
            sampleIdx = this.filenames.size();
            this.filenames.add(s.filename);
        }
        s.instrumentId = this.id;
        s.id = insertIdx;
        s.sampleIndex = sampleIdx;

        samples.add(s);
    }

    protected Instrument(Parcel in) {
        projectId = in.readInt();
        name = in.readString();
        id = in.readInt();
        filenames = in.createStringArrayList();
        samples = in.createTypedArrayList(Sample.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(projectId);
        dest.writeString(name);
        dest.writeInt(id);
        dest.writeStringList(filenames);
        dest.writeTypedList(samples);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Instrument> CREATOR = new Creator<Instrument>() {
        @Override
        public Instrument createFromParcel(Parcel in) {
            return new Instrument(in);
        }

        @Override
        public Instrument[] newArray(int size) {
            return new Instrument[size];
        }
    };


    public String firstFilename() {
        if(this.filenames.size() < 1) {
            return null;
        }
        return this.filenames.get(0);
    }
}
