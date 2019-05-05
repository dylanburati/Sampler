package libre.sampler.models;

public class PatternEvent {
    public static final int PATTERN_ON = 1;
    public static final int PATTERN_OFF = 0;

    public int action;

    public PatternEvent(int action) {
        this.action = action;
    }
}
