package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;
import libre.sampler.utils.IdStatus;

@Entity(tableName = "instrument", primaryKeys = {"projectId", "id"})
public class Instrument implements Parcelable {
    public int projectId;
    public int id;
    public String name;

    @Ignore
    private List<Sample> samples = new ArrayList<>();
    @Ignore
    private String firstFilename;
    @Ignore
    private int nextSampleId;
    @Ignore
    private IdStatus idStatus = new IdStatus("Instrument");

    @Ignore
    public Instrument(String name) {
        this.name = name;
        this.nextSampleId = 0;
    }

    // should be called with the `id` obtained from the database
    public Instrument(String name, int id) {
        this.name = name;
        this.id = id;
        this.nextSampleId = 0;

        idStatus.set(IdStatus.SELF);
    }

    public void setInstrumentId(int id) {
        this.id = id;

        idStatus.set(IdStatus.SELF);
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
        this.firstFilename = null;
        for(Sample s : samples) {
            s.instrumentId = this.id;
            if(firstFilename == null) {
                firstFilename = s.filename;
            }
            if(s.id >= nextSampleId) {
                nextSampleId = s.id + 1;
            }
        }

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public Sample addSample(String filename) {
        Sample s = new Sample(filename, nextSampleId);
        s.instrumentId = this.id;
        samples.add(s);

        if(firstFilename == null) {
            firstFilename = s.filename;
        }
        nextSampleId++;

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
        return s;
    }

    public void addSample(Sample s) {
        s.instrumentId = this.id;
        s.id = nextSampleId;

        samples.add(s);
        if(firstFilename == null) {
            firstFilename = s.filename;
        }
        nextSampleId++;

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public List<Sample> getSamples() {
        return samples;
    }

    protected Instrument(Parcel in) {
        projectId = in.readInt();
        name = in.readString();
        id = in.readInt();
        firstFilename = in.readString();
        samples = in.createTypedArrayList(Sample.CREATOR);
        nextSampleId = in.readInt();
        idStatus = in.readParcelable(IdStatus.class.getClassLoader());
    }

    public String firstFilename() {
        return firstFilename;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(projectId);
        dest.writeString(name);
        dest.writeInt(id);
        dest.writeString(firstFilename);
        dest.writeTypedList(samples);
        dest.writeInt(nextSampleId);
        dest.writeParcelable(idStatus, flags);
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
}
