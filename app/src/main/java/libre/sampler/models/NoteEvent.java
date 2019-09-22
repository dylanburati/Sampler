package libre.sampler.models;

import libre.sampler.utils.NoteId;

public class NoteEvent {
    public static final int NOTHING = -1;
    public static final int NOTE_OFF = 0;
    public static final int NOTE_ON = 1;
    public static final int CLOSE = 3;

    public int action;
    public Instrument instrument;
    public int keyNum;
    public int velocity;
    public NoteId eventId;

    public NoteEvent(int action, Instrument instrument, int keyNum, int velocity, NoteId eventId) {
        this.instrument = instrument;
        this.keyNum = keyNum;
        this.action = action;
        this.velocity = velocity * (action == 1 ? 1 : 0);
        this.eventId = eventId;
    }
}