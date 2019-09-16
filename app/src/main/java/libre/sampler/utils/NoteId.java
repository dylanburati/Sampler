package libre.sampler.utils;

import android.util.Pair;

public class NoteId {
    private static final long TIME_MASK             = 0x0000ffffffffffffL;
    private static final long SCHEDULED_MASK        = 0x0001000000000000L;
    private static final long DUPLICATE_MASK        = 0xfff0000000000000L;
    private static final int DUPLICATE_BIT_SHIFT = 52;

    public static Pair<Long, Integer> createForKeyboard(long timeMs, int pointerIndex) {
        return new Pair<>(timeMs & TIME_MASK, pointerIndex);
    }

    public static Pair<Long, Integer> createForMidi(int keyNum) {
        return new Pair<>(-1L, keyNum);
    }

    public static long createForScheduledNoteEvent(long timeMs, int duplicateIndex) {
        long id = timeMs & TIME_MASK;
        id |= SCHEDULED_MASK;
        id |= ((long) duplicateIndex << DUPLICATE_BIT_SHIFT) & DUPLICATE_MASK;
        return id;
    }

    public static long createDuplicate(long noteId) {
        long duplicateIndex = 1 + (noteId >> DUPLICATE_BIT_SHIFT);
        noteId &= TIME_MASK;
        noteId |= SCHEDULED_MASK;
        noteId |= (duplicateIndex << DUPLICATE_BIT_SHIFT) & DUPLICATE_MASK;
        return noteId;
    }
}
