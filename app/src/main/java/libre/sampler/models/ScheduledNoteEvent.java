package libre.sampler.models;

import android.util.Pair;

public class ScheduledNoteEvent {
    public Long offsetTicks;

    public int action;
    public Instrument instrument;
    public int keyNum;
    public int velocity;
    public long baseId;

    public ScheduledNoteEvent(Long offsetTicks, int action, Instrument instrument, int keyNum, int velocity, long baseId) {
        this.offsetTicks = offsetTicks;
        this.action = action;
        this.instrument = instrument;
        this.keyNum = keyNum;
        this.velocity = velocity;
        this.baseId = baseId;
    }

    public NoteEvent getNoteEvent(int loopIndex) {
        return new NoteEvent(action, instrument, keyNum, velocity, new Pair<>(baseId, loopIndex));
    }
}
