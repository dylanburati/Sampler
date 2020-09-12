package libre.sampler.views;

import androidx.annotation.NonNull;
import libre.sampler.models.ScheduledNoteEvent;

public class VisualNote implements Comparable<VisualNote> {
    public final ScheduledNoteEvent eventOn;
    public final ScheduledNoteEvent eventOff;
    public Long tag;

    public int modificationCount;

    public VisualNote(ScheduledNoteEvent eventOn, ScheduledNoteEvent eventOff) {
        this.eventOn = eventOn;
        this.eventOff = eventOff;

        this.tag = eventOn.noteId;
    }

    @Override
    public int compareTo(@NonNull VisualNote o) {
        int cmp = Long.compare(getStartTicks(), o.getStartTicks());
        if(cmp != 0) {
            return cmp;
        }
        return Long.compare(tag, o.tag);
    }

    public long getStartTicks() {
        return eventOn.offsetTicks;
    }

    public void moveTicks(long moveTicks) {
        this.eventOn.offsetTicks += moveTicks;
        this.eventOff.offsetTicks += moveTicks;
    }

    public long getLengthTicks() {
        return getEndTicks() - getStartTicks();
    }

    public void setLengthTicks(long lengthTicks) {
        this.eventOff.offsetTicks = this.eventOn.offsetTicks + lengthTicks;
    }

    public long getEndTicks() {
        return this.eventOff.offsetTicks;
    }

    public int getContainerIndex() {
        return 9 - (getKeyNum() / 12);
    }

    public int getKeyIndex() {
        return 11 - (getKeyNum() % 12);
    }

    public int getKeyNum() {
        return eventOn.keyNum;
    }

    public void setKeyNum(int keyNum) {
        this.eventOn.keyNum = keyNum;
        this.eventOff.keyNum = keyNum;
    }

    public int getVelocity() {
        return eventOn.velocity;
    }

    public void setVelocity(int velocity) {
        this.eventOn.velocity = velocity;
    }
}
