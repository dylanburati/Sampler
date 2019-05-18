package libre.sampler.models;

public class PatternEvent {
    public static final int PATTERN_ON = 1;
    public static final int PATTERN_OFF = 0;
    public static final int PATTERN_SELECT = 2;

    public int action;
    public Pattern pattern;

    public PatternEvent(int action, Pattern pattern) {
        this.action = action;
        this.pattern = pattern;
    }
}
