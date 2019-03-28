package libre.sampler.models;

public class NoteEvent {
    public static final int ACTION_BEGIN = 0;
    public static final int ACTION_END = 1;

    public int keyNum;
    public int action;

    public NoteEvent(int keyNum, int action) {
        this.keyNum = keyNum;
        this.action = action;
    }
}