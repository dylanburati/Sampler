package libre.sampler.utils;

import android.util.Log;

import java.util.List;

import libre.sampler.models.NoteEvent;
import libre.sampler.models.ScheduledNoteEvent;

public class LoopScheduler {
    private List<ScheduledNoteEvent> events;
    private int loopIndex;
    private int eventIndex;

    /**
     * Every event before this has been dispatched, every event after has not.
     */
    private int pendingEventIndex;
    private int pendingLoopIndex;

    public long loopLengthTicks;

    public LoopScheduler(List<ScheduledNoteEvent> events, long loopLengthTicks) {
        this.events = events;
        this.loopLengthTicks = loopLengthTicks;
        if(events.size() == 0) {
            throw new AssertionError("LoopScheduler initialized with empty event loop");
        }
        this.pendingLoopIndex = 0;
        this.pendingEventIndex = 0;
        this.loopIndex = 0;
        this.eventIndex = 0;
    }

    public synchronized void reset() {
        this.pendingLoopIndex = 0;
        this.pendingEventIndex = 0;
        this.loopIndex = 0;
        this.eventIndex = 0;
    }

    private synchronized void incrementIndex() {
        eventIndex++;
        if(eventIndex >= events.size()) {
            loopIndex++;
            eventIndex = 0;
        }
    }

    public synchronized void decrementIndex() {
        eventIndex--;
        if(eventIndex < 0) {
            loopIndex--;
            eventIndex = events.size() - 1;
        }
    }

    public synchronized long getNextEventTicks() {
        ScheduledNoteEvent n = events.get(eventIndex);
        return getEventTicks(n);
    }

    public synchronized long getEventTicks(ScheduledNoteEvent n) {
        return n.offsetTicks + loopIndex * loopLengthTicks;
    }

    public synchronized void cancelPending() {
        // if(loopIndex != pendingLoopIndex || eventIndex != pendingEventIndex)
        loopIndex = pendingLoopIndex;
        eventIndex = pendingEventIndex;
    }

    public synchronized void confirmPending() {
        Log.d("LoopScheduler", String.format("Advance from %d:%d to %d:%d", pendingLoopIndex, pendingEventIndex, loopIndex, eventIndex));
        pendingLoopIndex = loopIndex;
        pendingEventIndex = eventIndex;
    }

    public synchronized void doInsert(int insertIdx, ScheduledNoteEvent evt, long ticksNow) {
        events.add(insertIdx, evt);
        if(pendingEventIndex >= insertIdx) {
            pendingEventIndex++;
        }
        int insertedLoopIndex = (int) (ticksNow / loopLengthTicks);
        long insertedEventTicks = evt.offsetTicks + insertedLoopIndex * loopLengthTicks;

        if(insertedEventTicks > ticksNow) {
            if(insertedLoopIndex < pendingLoopIndex ||
                    (insertedLoopIndex == pendingLoopIndex && insertIdx < pendingEventIndex)) {

                pendingLoopIndex = insertedLoopIndex;
                pendingEventIndex = insertIdx;
            }
        }

        cancelPending();
    }

    public synchronized NoteEvent doRemove(int removeIdx, ScheduledNoteEvent evt) {
        int lastScheduleLoopIndex = pendingLoopIndex;
        if(removeIdx < pendingEventIndex) {
            pendingEventIndex--;
            // removed event was already dispatched in this loop
            lastScheduleLoopIndex++;
        } else if(removeIdx == pendingEventIndex && removeIdx == events.size() - 1) {
            // pendingEventIndex points to end of list
            pendingLoopIndex++;
            pendingEventIndex = 0;
        }
        events.remove(removeIdx);
        if(events.size() == 0) {
            events.add(0, ScheduledNoteEvent.getPlaceholder());
            Log.d("LoopScheduler", "Warning: placeholder event missing was missing from pattern");
        }
        cancelPending();

        return evt.getNoteEvent(lastScheduleLoopIndex);
    }

    public synchronized NoteEvent getNextNoteEvent() {
        ScheduledNoteEvent n = events.get(eventIndex);
        NoteEvent noteEvent = n.getNoteEvent(loopIndex);
        incrementIndex();
        return noteEvent;
    }

    public int getLoopIndex() {
        return loopIndex;
    }
}
