package libre.sampler.models;

public class Sample {
    public int id;
    public String filename;
    public int sampleIndex;
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

    public boolean isLoaded = false;
    public boolean isLoopConfigured = false;

    public Sample(String filename, int sampleIndex, int id) {
        this.filename = filename;
        this.sampleIndex = sampleIndex;
        this.id = id;
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
        this.isLoopConfigured = true;
    }

    public boolean setDefaultLoop() {
        if(!this.isLoaded) {
            return false;
        }
        this.startTime = 0;
        this.resumeTime = -1;
        this.endTime = sampleLength / 1.0f / sampleRate;
        this.isLoopConfigured = true;
        return true;
    }
}
