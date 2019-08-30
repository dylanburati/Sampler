package libre.sampler.models;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "scheduledNoteEvent", primaryKeys = {"patternId", "id"})
public class ScheduledNoteEvent {
    public int id;
    public int patternId;

    public long offsetTicks;

    public int action;
    @Ignore
    public Instrument instrument;
    public int instrumentId;
    public int keyNum;
    public int velocity;
    public long noteId;

    // should be called with the `id` obtained from the database
    public ScheduledNoteEvent(int id, int instrumentId) {
        this.id = id;
        this.instrumentId = instrumentId;
    }

    @Ignore
    public ScheduledNoteEvent(long offsetTicks, int action, Instrument instrument, int keyNum, int velocity, long noteId) {
        this.id = 0;

        this.offsetTicks = offsetTicks;
        this.action = action;
        this.instrument = instrument;
        if(instrument != null) {
            this.instrumentId = instrument.id;
        }
        this.keyNum = keyNum;
        this.velocity = velocity;
        this.noteId = noteId;
    }

    public static ScheduledNoteEvent getPlaceholder() {
        return new ScheduledNoteEvent(0L, NoteEvent.NOTHING, null, 0, 0, 0);
    }

    public NoteEvent getNoteEvent(int loopIndex) {
        return new NoteEvent(action, instrument, keyNum, velocity, new Pair<>(noteId, loopIndex));
    }

    public void setInstrument(Instrument t) {
        instrument = t;
        instrumentId = t.id;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("ScheduledNoteEvent(id=%d, instrumentId=%d, noteId=%016x, offsetTicks=%d)",
                this.id, this.instrumentId, this.noteId, this.offsetTicks);
    }

    public int valueHash() {
        int hashCode = 0;
        hashCode ^= id;
        hashCode ^= patternId;
        hashCode ^= (int) (offsetTicks % 0x80000000L);
        hashCode ^= instrumentId;
        hashCode ^= (action << 20);
        hashCode ^= (keyNum << 16);
        hashCode ^= (velocity << 8);
        hashCode ^= (int) (noteId % 0x80000000L);

        return hashCode;
    }
}
