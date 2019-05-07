package libre.sampler.models;

public class ScheduledNoteEvent {
    public Long offsetTicks;
    public NoteEvent event;

    public ScheduledNoteEvent(Long offsetTicks, NoteEvent event) {
        this.offsetTicks = offsetTicks;
        this.event = event;
    }
}
