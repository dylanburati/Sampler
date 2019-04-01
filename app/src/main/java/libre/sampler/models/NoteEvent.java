package libre.sampler.models;

import android.util.Pair;

public class NoteEvent {
    public static final int ACTION_BEGIN = 0;
    public static final int ACTION_END = 1;

    public int keyNum;
    public int action;
    public Pair<Long, Integer> eventId;

    public NoteEvent(int keyNum, int action, Pair<Long, Integer> eventId) {
        this.keyNum = keyNum;
        this.action = action;
        this.eventId = eventId;
    }
}