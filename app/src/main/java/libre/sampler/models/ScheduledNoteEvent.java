package libre.sampler.models;

import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import libre.sampler.utils.NoteId;

@Entity(tableName = "scheduledNoteEvent")
public class ScheduledNoteEvent {
    public String patternId;

    @PrimaryKey
    @NonNull
    public String id;

    public long offsetTicks;

    public int action;
    @Ignore
    public Instrument instrument;
    public String instrumentId;
    public int keyNum;
    public int velocity;
    public long noteId;

    // should be called with the `id` obtained from the database
    public ScheduledNoteEvent(@NonNull String id, String instrumentId) {
        this.id = id;
        this.instrumentId = instrumentId;
    }

    @Ignore
    public ScheduledNoteEvent(long offsetTicks, int action, Instrument instrument, int keyNum, int velocity, long noteId) {
        this.id = UUID.randomUUID().toString();

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
        return new NoteEvent(action, instrument, keyNum, velocity, new NoteId(noteId, loopIndex));
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
        if (id != null) hashCode ^= id.hashCode();
        if (patternId != null) hashCode ^= patternId.hashCode();
        hashCode ^= (int) (offsetTicks % 0x80000000L);
        if (instrumentId != null) hashCode ^= instrumentId.hashCode();
        hashCode ^= (action << 20);
        hashCode ^= (keyNum << 16);
        hashCode ^= (velocity << 8);
        hashCode ^= (int) (noteId % 0x80000000L);

        return hashCode;
    }
}
