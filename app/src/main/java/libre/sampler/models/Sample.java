package libre.sampler.models;

public class Sample {
    public int id;
    public String filename;
    public int sampleRate;

    public float attack;
    public float decay;
    public float sustain;
    public float release;

    public int basePitch;

    public float startTime;
    public float resumeTime;
    public float endTime;

    public boolean loaded = false;

    public Sample(int id) {
        this.id = id;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setSampleRate(int sampleRate) {
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
    }
}
