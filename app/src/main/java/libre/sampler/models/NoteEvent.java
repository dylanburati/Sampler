package libre.sampler.models;

import android.util.Pair;

public class NoteEvent {
    public static final int NOTE_ON = 0;
    public static final int NOTE_OFF = 1;

    public int keyNum;
    public int velocity;
    public int action;
    public Pair<Long, Integer> eventId;

    public NoteEvent(int action, int keyNum, int velocity, Pair<Long, Integer> eventId) {
        this.keyNum = keyNum;
        this.action = action;
        this.velocity = velocity * (action == 1 ? 0 : 1);
        this.eventId = eventId;
    }
}