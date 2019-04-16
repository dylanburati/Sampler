package libre.sampler.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "sample", primaryKeys = {"instrumentId", "id"})
public class Sample implements Parcelable {
    public int instrumentId;

    public int id;
    public String filename;

    @Ignore
    public int sampleIndex;

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

    @Ignore
    public boolean isInfoLoaded = false;

    public Sample(String filename, int sampleIndex, int id) {
        this.filename = filename;
        this.sampleIndex = sampleIndex;
        this.id = id;
        this.setSampleZone(-1, -1, 0, 128);
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
    }

    public void setEnvelope(float attack, float decay, float sustain, float release) {
        this.attack = attack;
        this.decay = decay;
        this.sustain = sustain;
        this.release = release;
    }

    public void setBasePitch(int basePitch) {
        this.basePitch = basePitch;
    }

    public void setLoop(float startTime, float resumeTime, float endTime) {
        this.startTime = startTime;
        this.resumeTime = resumeTime;
        this.endTime = endTime;
        this.shouldUseDefaultLoopStart = this.shouldUseDefaultLoopResume = this.shouldUseDefaultLoopEnd = false;
    }

    public void setLoopStart(float startTime) {
        this.startTime = startTime;
        this.shouldUseDefaultLoopStart = false;
    }

    public void setLoopResume(float resumeTime) {
        this.resumeTime = resumeTime;
        this.shouldUseDefaultLoopResume = false;
    }

    public void setLoopEnd(float endTime) {
        this.endTime = endTime;
        this.shouldUseDefaultLoopEnd = false;
    }

    public float getStartTime() {
        if(shouldUseDefaultLoopStart) {
            return 0;
        }
        return startTime;
    }

    public float getResumeTime() {
        if(shouldUseDefaultLoopResume) {
            return -1;
        }
        return resumeTime;
    }

    public float getEndTime() {
        if(shouldUseDefaultLoopEnd) {
            this.endTime = sampleLength / 1.0f / sampleRate;
        }
        return endTime;
    }

    public boolean contains(NoteEvent e) {
        if(e.keyNum < this.minPitch || e.keyNum > this.maxPitch ||
                e.velocity < this.minVelocity || e.velocity > this.maxVelocity) {
            return false;
        }
        return true;
    }

    public boolean checkLoop() {
        float loopLength0 = this.getEndTime() - this.getStartTime();
        if(Math.abs(loopLength0) < 1e-4) {
            return false;
        }
        if(this.resumeTime >= 0) {
            float loopLength1 = this.getEndTime() - this.getResumeTime();
            if(Math.abs(loopLength1) < 1e-4) {
                return false;
            }
        }
        return true;
    }

    public Sample() {
    }

    protected Sample(Parcel in) {
        instrumentId = in.readInt();
        id = in.readInt();
        filename = in.readString();
        sampleIndex = in.readInt();
        minPitch = in.readInt();
        maxPitch = in.readInt();
        minVelocity = in.readInt();
        maxVelocity = in.readInt();
        sampleLength = in.readInt();
        sampleRate = in.readInt();
        attack = in.readFloat();
        decay = in.readFloat();
        sustain = in.readFloat();
        release = in.readFloat();
        basePitch = in.readInt();
        startTime = in.readFloat();
        resumeTime = in.readFloat();
        endTime = in.readFloat();
        isInfoLoaded = in.readByte() != 0;
        shouldUseDefaultLoopStart = in.readByte() != 0;
        shouldUseDefaultLoopResume = in.readByte() != 0;
        shouldUseDefaultLoopEnd = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(instrumentId);
        dest.writeInt(id);
        dest.writeString(filename);
        dest.writeInt(sampleIndex);
        dest.writeInt(minPitch);
        dest.writeInt(maxPitch);
        dest.writeInt(minVelocity);
        dest.writeInt(maxVelocity);
        dest.writeInt(sampleLength);
        dest.writeInt(sampleRate);
        dest.writeFloat(attack);
        dest.writeFloat(decay);
        dest.writeFloat(sustain);
        dest.writeFloat(release);
        dest.writeInt(basePitch);
        dest.writeFloat(startTime);
        dest.writeFloat(resumeTime);
        dest.writeFloat(endTime);
        dest.writeByte((byte) (isInfoLoaded ? 1 : 0));
        dest.writeByte((byte) (shouldUseDefaultLoopStart ? 1 : 0));
        dest.writeByte((byte) (shouldUseDefaultLoopResume ? 1 : 0));
        dest.writeByte((byte) (shouldUseDefaultLoopEnd ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Sample> CREATOR = new Creator<Sample>() {
        @Override
        public Sample createFromParcel(Parcel in) {
            return new Sample(in);
        }

        @Override
        public Sample[] newArray(int size) {
            return new Sample[size];
        }
    };

}
