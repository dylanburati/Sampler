package libre.sampler.models;

import java.util.ArrayList;
import java.util.List;

import androidx.room.Entity;
import androidx.room.Ignore;
import libre.sampler.utils.IdStatus;

@Entity(tableName = "instrument", primaryKeys = {"projectId", "id"})
public class Instrument {
    public int projectId;
    public int id;
    public String name;
    private float volume;

    @Ignore
    private List<Sample> samples = new ArrayList<>();
    @Ignore
    private int nextSampleId;
    @Ignore
    private IdStatus idStatus = new IdStatus("Instrument,Sample");

    @Ignore
    public Instrument(String name) {
        this.name = name;
        this.nextSampleId = 0;
        this.volume = 1.0f;
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
        for(Sample s : samples) {
            s.instrumentId = this.id;
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

        nextSampleId++;

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
        return s;
    }

    public void addSample(Sample s) {
        s.instrumentId = this.id;
        s.id = nextSampleId;
        samples.add(s);

        nextSampleId++;

        idStatus.require(IdStatus.SELF);
        idStatus.set(IdStatus.CHILDREN_ADDED);
    }

    public void removeSample(Sample toRemove) {
        samples.remove(toRemove);
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public List<Sample> getSamplesForEvent(NoteEvent event) {
        List<Sample> retval = new ArrayList<>(10);
        for(int i = 0; i < samples.size(); i++) {
            if(samples.get(i).contains(event)) {
                retval.add(samples.get(i));
            }
        }
        return retval;
    }

    public float getVolume() {
        return volume;
    }

    public float getVolumeDecibels() {
        return 20 * (float) Math.log10((double) this.volume);
    }

    public void setVolume(float volume) {
        this.volume = volume;
    }

    public void setVolumeDecibels(float volumeDecibels) {
        if(volumeDecibels >= 0) {
            this.volume = 1;
        } else {
            // dB to amplitude
            this.volume = (float) Math.pow(10, volumeDecibels / 20.0);
        }
    }
}
