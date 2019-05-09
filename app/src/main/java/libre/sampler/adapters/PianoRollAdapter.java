package libre.sampler.adapters;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.PianoRollSettings;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.publishers.PatternBuilder;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.RepeatingDrawable;

public class PianoRollAdapter extends RecyclerView.Adapter<PianoRollAdapter.ViewHolder> {
    public static final int SPAN_COUNT = 8;

    public int barWidth;
    public float keyHeight;

    private List<PianoRollNote> pianoRollNotes;
    private PianoRollSettings pianoRollSettings;
    private PatternBuilder patternBuilder;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private RelativeLayout notePane;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            notePane = v.findViewById(R.id.piano_roll_column);
        }
    }

    public PianoRollAdapter(PianoRollSettings pianoRollSettings, PatternBuilder patternBuilder) {
        this.pianoRollSettings = pianoRollSettings;
        this.patternBuilder = patternBuilder;
        this.pianoRollNotes = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_piano_roll_column, parent, false);
        v.findViewById(R.id.piano_roll_column).setBackground(
                new RepeatingDrawable(parent.getContext().getDrawable(R.drawable.piano_roll_column), RepeatingDrawable.HORIZONTAL));

        if(barWidth == 0) {
            barWidth = parent.getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth);
        }
        if(keyHeight == 0) {
            keyHeight = parent.getResources().getDimensionPixelOffset(R.dimen.piano_roll_colheight) / 12.0f;
        }
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.notePane.removeAllViews();
        for(PianoRollNote n : pianoRollNotes) {
            if(n.containerIndex == position) {
                displayNote(holder.notePane, n);
            }
        }

        final GestureDetectorCompat detector = new GestureDetectorCompat(holder.notePane.getContext(),
                new MyGestureListener(holder));
        holder.notePane.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return SPAN_COUNT;
    }

    private void displayNote(RelativeLayout parent, PianoRollNote note) {
        note.view = new View(parent.getContext());
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(note.width, (int) keyHeight);
        params.leftMargin = note.left;
        params.topMargin = note.top;
        note.view.setBackgroundColor(Color.WHITE);
        parent.addView(note.view, params);
    }

    private PianoRollNote convertToNote(int containerIndex, int keyIndex, float x, int containerWidth) {
        float userTickWidth = barWidth / 1.0f / AppConstants.USER_TICKS_PER_BEAT / AppConstants.BEATS_PER_BAR;
        float inputUserTicks = x / userTickWidth;
        int snapUserTicks = pianoRollSettings.snapUserTicks;

        int startUserTicks = Math.round(inputUserTicks / 1.0f / snapUserTicks - 0.3f) * snapUserTicks;

        int maxUserTicks = pianoRollSettings.containerLengthUserTicks - startUserTicks;
        int lengthUserTicks = pianoRollSettings.lengthUserTicks;
        if(lengthUserTicks > maxUserTicks) {
            lengthUserTicks = (int) Math.floor(maxUserTicks / 1.0f / snapUserTicks) * snapUserTicks;
        }

        PianoRollNote note = new PianoRollNote(containerIndex, keyIndex, startUserTicks * userTickWidth, lengthUserTicks * userTickWidth);
        notifyNoteCreated(note, startUserTicks, lengthUserTicks);
        return note;
    }

    private void notifyNoteCreated(PianoRollNote note, int startUserTicks, int lengthUserTicks) {
        note.id = patternBuilder.getNextNoteId();
        note.eventOn = new ScheduledNoteEvent((long) (startUserTicks * AppConstants.TICKS_PER_USER_TICK),
                NoteEvent.NOTE_ON, note.keyNum, pianoRollSettings.velocity, note.id);
        note.eventOff = new ScheduledNoteEvent((long) ((startUserTicks + lengthUserTicks) * AppConstants.TICKS_PER_USER_TICK),
                NoteEvent.NOTE_OFF, note.keyNum, pianoRollSettings.velocity, note.id);
        patternBuilder.add(note.eventOn, note.eventOff);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private ViewHolder holder;

        public MyGestureListener(ViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            int containerIndex = holder.getAdapterPosition();
            int keyIndex = (int) (e.getY() / keyHeight);

            Rect r = new Rect();
            for(PianoRollNote n : pianoRollNotes) {
                if(n.containerIndex == containerIndex && n.view != null) {
                    n.view.getLocalVisibleRect(r);
                    holder.notePane.offsetDescendantRectToMyCoords(n.view, r);
                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        holder.notePane.removeView(n.view);
                        pianoRollNotes.remove(n);
                        patternBuilder.remove(n.eventOn, n.eventOff);
                        return;
                    }
                }
            }

            int containerWidth = holder.notePane.getWidth();
            PianoRollNote note = convertToNote(containerIndex, keyIndex, e.getX(), containerWidth);
            pianoRollNotes.add(note);

            // if(note.left + note.width > containerWidth) {
            //     ViewGroup.LayoutParams containerParams = holder.notePane.getLayoutParams();
            //     int containerBaseWidth = holder.notePane.getResources().getDimensionPixelOffset(R.dimen.piano_roll_barwidth);
            //     int nExpand = 1 + (note.left + note.width - containerWidth) / containerBaseWidth;
            //     containerParams.width = containerWidth + nExpand * containerBaseWidth;
            //     holder.notePane.setLayoutParams(containerParams);
            //     Log.d("PianoRollAdapter", "expanded");
            // }

            displayNote(holder.notePane, note);
        }
    }

    public class PianoRollNote {
        public int containerIndex;
        public int keyNum;
        public int id;

        public int left;
        public int top;
        public int width;

        public View view;

        public ScheduledNoteEvent eventOn;
        public ScheduledNoteEvent eventOff;

        public PianoRollNote(int containerIndex, int keyIndex, float x, float width) {
            this.containerIndex = containerIndex;
            this.keyNum = (9 - containerIndex) * 12 + (11 - keyIndex);
            this.top = (int) (keyIndex * keyHeight);
            this.left = (int) x;
            this.width = (int) width;
        }
    }
}
