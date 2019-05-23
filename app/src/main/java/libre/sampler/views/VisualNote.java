package libre.sampler.views;

import libre.sampler.models.ScheduledNoteEvent;

public class VisualNote {
    public ScheduledNoteEvent eventOn;
    public ScheduledNoteEvent eventOff;
    public Long tag;

    public long startTicks;
    public long lengthTicks;
    public int containerIndex;
    public int keyIndex;

    public VisualNote(int containerIndex) {
        this.containerIndex = containerIndex;
    }

    public VisualNote(ScheduledNoteEvent eventOn) {
        this.eventOn = eventOn;
    }

    public void calculateParams() {
        this.tag = eventOn.noteId;
        this.startTicks = eventOn.offsetTicks;
        this.lengthTicks = eventOff.offsetTicks - this.startTicks;
        this.containerIndex = 9 - (eventOn.keyNum / 12);
        this.keyIndex = 11 - (eventOn.keyNum % 12);
    }
}
