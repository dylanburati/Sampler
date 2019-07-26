package libre.sampler.views;

import libre.sampler.models.ScheduledNoteEvent;

public class VisualNote implements Comparable<VisualNote> {
    public final ScheduledNoteEvent eventOn;
    public final ScheduledNoteEvent eventOff;
    public Long tag;

    public long startTicks;
    public long lengthTicks;
    public int containerIndex;
    public int keyIndex;
    public int modificationCount;

    public VisualNote(ScheduledNoteEvent eventOn, ScheduledNoteEvent eventOff) {
        this.eventOn = eventOn;
        this.eventOff = eventOff;

        this.tag = eventOn.noteId;
        this.startTicks = eventOn.offsetTicks;
        this.lengthTicks = eventOff.offsetTicks - eventOn.offsetTicks;
        this.containerIndex = 9 - (eventOn.keyNum / 12);
        this.keyIndex = 11 - (eventOn.keyNum % 12);
    }

    @Override
    public int compareTo(VisualNote o) {
        int cmp = Long.compare(startTicks, o.startTicks);
        if(cmp != 0) {
            return cmp;
        }
        return Long.compare(tag, o.tag);
    }
}
