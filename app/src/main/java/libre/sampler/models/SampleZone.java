package libre.sampler.models;

public class SampleZone implements Comparable<NoteEvent> {
    public int minPitch;
    public int maxPitch;
    public int minVelocity;
    public int maxVelocity;

    public SampleZone(int minPitch, int maxPitch, int minVelocity, int maxVelocity) {
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.minVelocity = minVelocity;
        this.maxVelocity = maxVelocity;
    }

    @Override
    public int compareTo(NoteEvent o) {
        if(o.keyNum < this.minPitch) {
            return -2;
        }
        if(o.keyNum > this.maxPitch) {
            return 2;
        }
        if(o.velocity < this.minVelocity) {
            return -1;
        }
        if(o.velocity > this.maxVelocity) {
            return 1;
        }
        return 0;
    }

    public boolean contains(NoteEvent o) {
        return this.compareTo(o) == 0;
    }
}
