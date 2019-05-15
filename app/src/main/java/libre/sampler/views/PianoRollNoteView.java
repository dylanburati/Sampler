package libre.sampler.views;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;

import libre.sampler.models.ScheduledNoteEvent;

public class PianoRollNoteView extends View {
    public int containerIndex;
    public RelativeLayout.LayoutParams layoutParams;
    public ScheduledNoteEvent eventOn;
    public ScheduledNoteEvent eventOff;

    public PianoRollNoteView(Context context) {
        super(context);
    }
}
