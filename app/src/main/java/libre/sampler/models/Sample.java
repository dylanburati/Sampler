package libre.sampler.models;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "sample", primaryKeys = {"instrumentId", "id"})
public class Sample {
    public int instrumentId;

    public int id;
    public String filename;

    @Ignore
    public int sampleIndex;

    private float volume;
    public int minPitch;
    public int maxPitch;
    public int minVelocity;
    public int maxVelocity;

    @Ignore
    public int sampleLength;
    @Ignore
    public int sampleRate;

    public float attack;
    public float decay;
    public float sustain;
    public float release;

    public int basePitch;

    public float startTime;
    public float resumeTime;
    public float endTime;

    public boolean shouldUseDefaultLoopStart = true;
    public boolean shouldUseDefaultLoopResume = true;
    public boolean shouldUseDefaultLoopEnd = true;
    public int displayFlags;

    @Ignore
    private boolean isInfoLoaded = false;

    public Sample(String filename, int id) {
        this.filename = filename;
        this.id = id;
        this.sampleIndex = -1;

        this.setSampleZone(-1, -1, 0, 127);
        this.sustain = 1.0f;
        this.volume = 1.0f;
        this.displayFlags |= FIELD_MIN_PITCH | FIELD_MAX_PITCH;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        this.sampleIndex = -1;
    }

    public void setSampleZone(int minPitch, int maxPitch, int minVelocity, int maxVelocity) {
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
    }

    public void setSampleInfo(int sampleLength, int sampleRate) {
        this.sampleLength = sampleLength;
        this.sampleRate = sampleRate;
        this.isInfoLoaded = true;
    }

    public boolean isInfoLoaded() {
        return this.isInfoLoaded;
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

    public void setMinPitch(int minPitch) {
        this.minPitch = minPitch;
        this.displayFlags |= FIELD_MIN_PITCH;
    }

    public void setMaxPitch(int maxPitch) {
        this.maxPitch = maxPitch;
        this.displayFlags |= FIELD_MAX_PITCH;
    }

    public void setBasePitch(int basePitch) {
        this.basePitch = basePitch;
        this.displayFlags |= FIELD_BASE_PITCH;
    }

    public void setMinVelocity(int minVelocity) {
        this.minVelocity = minVelocity;
        this.displayFlags |= FIELD_MIN_VELOCITY;
    }

    public void setMaxVelocity(int maxVelocity) {
        this.maxVelocity = maxVelocity;
        this.displayFlags |= FIELD_MAX_VELOCITY;
    }

    public float getLoopStart() {
        if(shouldUseDefaultLoopStart) {
            return 0;
        }
        return startTime;
    }

    public void setLoopStart(float startTime) {
        this.startTime = startTime;
        this.shouldUseDefaultLoopStart = false;
        this.displayFlags |= FIELD_LOOP_START;
    }

    public float getLoopEnd() {
        if(shouldUseDefaultLoopEnd) {
            this.endTime = sampleLength / 1.0f / sampleRate;
        }
        return endTime;
    }

    public void setLoopEnd(float endTime) {
        this.endTime = endTime;
        this.shouldUseDefaultLoopEnd = false;
        this.displayFlags |= FIELD_LOOP_END;
    }

    public float getLoopResume() {
        if(shouldUseDefaultLoopResume) {
            return -1;
        }
        return resumeTime;
    }

    public void setLoopResume(float resumeTime) {
        this.resumeTime = resumeTime;
        this.shouldUseDefaultLoopResume = false;
        this.displayFlags |= FIELD_LOOP_RESUME;
    }

    public void setAttack(float attack) {
        this.attack = attack;
        this.displayFlags |= FIELD_ATTACK;
    }

    public void setDecay(float decay) {
        this.decay = decay;
        this.displayFlags |= FIELD_DECAY;
    }

    public float getSustainDecibels() {
        return 20 * (float) Math.log10((double) this.sustain);
    }

    public void setSustainDecibels(float sustainDecibels) {
        if(sustainDecibels >= 0) {
            this.sustain = 1;
        } else {
            // dB to amplitude
            this.sustain = (float) Math.pow(10, sustainDecibels / 20.0);
        }
    }

    public void setSustain(float sustain) {
        this.sustain = sustain;
        this.displayFlags |= FIELD_SUSTAIN;
    }

    public void setRelease(float release) {
        this.release = release;
        this.displayFlags |= FIELD_RELEASE;
    }

    public boolean contains(NoteEvent e) {
        if(e.keyNum < this.minPitch || e.keyNum > this.maxPitch ||
                e.velocity < this.minVelocity || e.velocity > this.maxVelocity) {
            return false;
        }
        return true;
    }

    public boolean checkLoop() {
        float loopLength0 = this.getLoopEnd() - this.getLoopStart();
        if(Math.abs(loopLength0) < 1e-4) {
            return false;
        }
        if(this.resumeTime >= 0) {
            float loopLength1 = this.getLoopEnd() - this.getLoopResume();
            if(Math.abs(loopLength1) < 1e-4) {
                return false;
            }
        }
        return true;
    }

    public static final int FIELD_MIN_PITCH = 0x01;
    public static final int FIELD_MAX_PITCH = 0x02;
    public static final int FIELD_BASE_PITCH = 0x04;
    public static final int FIELD_MIN_VELOCITY = 0x08;
    public static final int FIELD_MAX_VELOCITY = 0x10;
    public static final int FIELD_LOOP_START = 0x20;
    public static final int FIELD_LOOP_END = 0x40;
    public static final int FIELD_LOOP_RESUME = 0x80;
    public static final int FIELD_ATTACK = 0x100;
    public static final int FIELD_DECAY = 0x200;
    public static final int FIELD_SUSTAIN = 0x400;
    public static final int FIELD_RELEASE = 0x800;
    public boolean shouldDisplay(int field) {
        return (displayFlags & field) != 0;
    }
}
