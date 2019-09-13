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
    private int minPitch;
    private int maxPitch;
    private int minVelocity;
    private int maxVelocity;

    @Ignore
    private int sampleLength;
    @Ignore
    private int sampleRate;

    private float attack;
    private float decay;
    private float sustain;
    private float release;

    private float basePitch;

    private float startTime;
    private float resumeTime;
    private float endTime;

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
        this.displayFlags = FIELD_MIN_PITCH | FIELD_MAX_PITCH;
    }

    public Sample(Sample other) {
        this.id = -1;
        this.sampleIndex = -1;
        this.instrumentId = -1;

        this.filename = other.filename;
        this.volume = other.volume;
        this.minPitch = other.minPitch;
        this.maxPitch = other.maxPitch;
        this.minVelocity = other.minVelocity;
        this.maxVelocity = other.maxVelocity;
        this.attack = other.attack;
        this.decay = other.decay;
        this.sustain = other.sustain;
        this.release = other.release;
        this.basePitch = other.basePitch;
        this.startTime = other.startTime;
        this.resumeTime = other.resumeTime;
        this.endTime = other.endTime;
        this.displayFlags = other.displayFlags;
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

    public int getSampleLength() {
        return this.sampleLength;
    }

    public int getSampleRate() {
        return this.sampleRate;
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

    public int getMinPitch() {
        return this.minPitch;
    }

    public void setMinPitch(int minPitch) {
        this.minPitch = minPitch;
        this.displayFlags |= FIELD_MIN_PITCH;
    }

    public int getMaxPitch() {
        return this.maxPitch;
    }

    public void setMaxPitch(int maxPitch) {
        this.maxPitch = maxPitch;
        this.displayFlags |= FIELD_MAX_PITCH;
    }

    public float getBasePitch() {
        return this.basePitch;
    }

    public void setBasePitch(float basePitch) {
        this.basePitch = basePitch;
        this.displayFlags |= FIELD_BASE_PITCH;
    }

    public int getMinVelocity() {
        return this.minVelocity;
    }

    public void setMinVelocity(int minVelocity) {
        this.minVelocity = minVelocity;
        this.displayFlags |= FIELD_MIN_VELOCITY;
    }

    public int getMaxVelocity() {
        return this.maxVelocity;
    }

    public void setMaxVelocity(int maxVelocity) {
        this.maxVelocity = maxVelocity;
        this.displayFlags |= FIELD_MAX_VELOCITY;
    }

    public float getStartTime() {
        return this.startTime;
    }

    public float getLoopStart() {
        return startTime;
    }

    public void setStartTime(float startTime) {
        this.startTime = startTime;
        if(startTime > 0) {
            this.displayFlags |= FIELD_LOOP_START;
        } else {
            this.displayFlags &= (~FIELD_LOOP_START);
        }
    }

    public float getEndTime() {
        return this.endTime;
    }

    public float getLoopEnd() {
        if(endTime <= 0) {
            return (sampleLength / 1.0f / sampleRate);
        }
        return endTime;
    }

    public void setEndTime(float endTime) {
        this.endTime = endTime;
        if(endTime > 0) {
            this.displayFlags |= FIELD_LOOP_END;
        } else {
            this.displayFlags &= (~FIELD_LOOP_END);
        }
    }

    public float getResumeTime() {
        return this.resumeTime;
    }

    public float getLoopResume() {
        if(resumeTime <= 0) {
            return -1;
        }
        return resumeTime;
    }

    public void setResumeTime(float resumeTime) {
        this.resumeTime = resumeTime;
        if(resumeTime > 0) {
            this.displayFlags |= FIELD_LOOP_RESUME;
        } else {
            this.displayFlags &= (~FIELD_LOOP_RESUME);
        }
    }

    public float getAttack() {
        return this.attack;
    }

    public void setAttack(float attack) {
        this.attack = attack;
        this.displayFlags |= FIELD_ATTACK;
    }

    public float getDecay() {
        return this.decay;
    }

    public void setDecay(float decay) {
        this.decay = decay;
        this.displayFlags |= FIELD_DECAY;
    }

    public float getSustain() {
        return this.sustain;
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

    public float getRelease() {
        return this.release;
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

    public String getDisplayName() {
        return filename.replaceFirst("^.*/", "")
                .replaceFirst("\\.[0-9A-Za-z]{2,4}$", "")
                .replaceFirst("--[0-9a-f]{6,}$", "");
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

    public int getDisplayFlags() {
        return this.displayFlags;
    }

    public int valueHash() {
        int hashCode = 0;
        hashCode ^= this.id;
        hashCode ^= this.instrumentId;
        hashCode ^= this.filename.hashCode();
        hashCode ^= Float.floatToIntBits(this.volume);
        hashCode ^= this.minPitch;
        hashCode ^= this.maxPitch;
        hashCode ^= this.minVelocity;
        hashCode ^= this.maxVelocity;
        hashCode ^= Float.floatToIntBits(this.attack);
        hashCode ^= Float.floatToIntBits(this.decay);
        hashCode ^= Float.floatToIntBits(this.sustain);
        hashCode ^= Float.floatToIntBits(this.release);
        hashCode ^= Float.floatToIntBits(this.basePitch);
        hashCode ^= Float.floatToIntBits(this.startTime);
        hashCode ^= Float.floatToIntBits(this.resumeTime);
        hashCode ^= Float.floatToIntBits(this.endTime);
        hashCode ^= this.displayFlags;

        return hashCode;
    }
}
