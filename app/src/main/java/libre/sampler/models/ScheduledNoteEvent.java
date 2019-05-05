package libre.sampler.models;

public class ScheduledNoteEvent {
    public Long offsetNanos;
    public NoteEvent event;

    public ScheduledNoteEvent(Long offsetNanos, NoteEvent event) {
        this.offsetNanos = offsetNanos;
        this.event = event;
    }
}
