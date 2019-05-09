package libre.sampler.models;

import android.util.Pair;

public class ScheduledNoteEvent {
    public Long offsetTicks;

    private int action;
    private int keyNum;
    private int velocity;
    private int baseId;

    public ScheduledNoteEvent(Long offsetTicks, int action, int keyNum, int velocity, int baseId) {
        this.offsetTicks = offsetTicks;
        this.action = action;
        this.keyNum = keyNum;
        this.velocity = velocity;
        this.baseId = baseId;
    }

    public NoteEvent getNoteEvent(int loopIndex) {
        return new NoteEvent(action, keyNum, velocity, new Pair<>((long) baseId, loopIndex));
    }
}
