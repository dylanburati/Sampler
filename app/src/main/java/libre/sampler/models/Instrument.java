package libre.sampler.models;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import libre.sampler.utils.MD5OutputStream;

@Entity(tableName = "instrument")
public class Instrument {
    public String projectId;

    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public int sort;

    private float volume;

    @Ignore
    private List<Sample> samples = new ArrayList<>();

    @Ignore
    public Instrument(String name) {
        this.name = name;
        this.id = UUID.randomUUID().toString();
        this.volume = 1.0f;
    }

    // should be called with the `id` obtained from the database
    public Instrument(@NonNull String id, String name) {
        this.name = name;
        this.id = id;
    }

    public void setInstrumentId(String id) {
        this.id = id;
    }

    public void setSamples(List<Sample> samples) {
        this.samples = samples;
    }

    public Sample addSample(String filename) {
        Sample s = new Sample(filename);
        s.instrumentId = this.id;
        samples.add(s);

        return s;
    }

    public void addSample(Sample s) {
        s.instrumentId = this.id;
        samples.add(s);
    }

    public void removeSample(Sample toRemove) {
        samples.remove(toRemove);
    }

    public List<Sample> getSamples() {
        return samples;
    }

    public List<Sample> getSamplesForEvent(NoteEvent event, boolean compareVelocity) {
        List<Sample> retval = new ArrayList<>(10);
        for(int i = 0; i < samples.size(); i++) {
            if(samples.get(i).containsKey(event)) {
                if(!compareVelocity || samples.get(i).containsVelocity(event)) {
                    retval.add(samples.get(i));
                }
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

    public void writeHashCodes(MD5OutputStream outputStream) throws IOException {
        outputStream.writeInt(id.hashCode());
        if (projectId != null) outputStream.writeInt(projectId.hashCode());
        outputStream.writeInt(Float.floatToIntBits(volume));
        for(Sample s : samples) {
            outputStream.writeInt(s.valueHash());
        }
    }

    public void prepareSave() {
        for (int i = 0; i < this.samples.size(); i++) {
            Sample e = this.samples.get(i);
            e.sort = i;
        }
    }
}
