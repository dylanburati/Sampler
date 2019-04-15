package libre.sampler.models;

public class Sample {
    public int id;
    public String filename;
    public int sampleIndex;
    public SampleZone zone;

    public int sampleLength;
    public int sampleRate;

    public float attack;
    public float decay;
    public float sustain;
    public float release;

    public int basePitch;

    public float startTime;
    public float resumeTime;
    public float endTime;

    public boolean isInfoLoaded = false;
    public boolean shouldUseDefaultLoopStart = true;
    public boolean shouldUseDefaultLoopResume = true;
    public boolean shouldUseDefaultLoopEnd = true;

    public Sample(String filename, int sampleIndex, int id) {
        this.filename = filename;
        this.sampleIndex = sampleIndex;
        this.id = id;
        this.zone = new SampleZone(-1, -1, 0, 128);
    }

    public void setSampleZone(int minPitch, int maxPitch, int minVelocity, int maxVelocity) {
        this.zone = new SampleZone(minPitch, maxPitch, minVelocity, maxVelocity);
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

    public boolean setDefaultLoop() {
        if(!this.isInfoLoaded) {
            return false;
        }
        if(shouldUseDefaultLoopStart) this.startTime = 0;
        if(shouldUseDefaultLoopResume) this.resumeTime = -1;
        if(shouldUseDefaultLoopEnd) this.endTime = sampleLength / 1.0f / sampleRate;

        this.shouldUseDefaultLoopStart = this.shouldUseDefaultLoopResume = this.shouldUseDefaultLoopEnd = false;
        return true;
    }

    public boolean contains(NoteEvent e) {
        if(this.zone == null) {
            return false;
        }
        return this.zone.contains(e);
    }

    public boolean checkLoop() {
        float loopLength0 = this.endTime - this.startTime;
        if(loopLength0 < 1e-4) {
            return false;
        }
        if(this.resumeTime >= 0) {
            float loopLength1 = this.endTime - this.resumeTime;
            if(loopLength1 < 1e-4) {
                return false;
            }
        }
        return true;
    }
}
