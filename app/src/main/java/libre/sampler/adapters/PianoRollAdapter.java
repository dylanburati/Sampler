package libre.sampler.adapters;

import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.utils.PianoRollController;
import libre.sampler.utils.RepeatingDrawable;
import libre.sampler.views.PianoRollNoteView;

public class PianoRollAdapter extends RecyclerView.Adapter<PianoRollAdapter.ViewHolder> {
    public static final int SPAN_COUNT = 8;

    public List<PianoRollNoteView> pianoRollNotes;
    private PianoRollController pianoRollController;
    private Set<ViewHolder> viewHolderSet;
    private int rollWidth;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private RelativeLayout notePane;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            notePane = v.findViewById(R.id.piano_roll_column);
        }
    }

    public PianoRollAdapter(PianoRollController pianoRollController) {
        this.pianoRollController = pianoRollController;
        this.pianoRollNotes = new ArrayList<>();
        this.viewHolderSet = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_piano_roll_column, parent, false);
        v.findViewById(R.id.piano_roll_column).setBackground(
                new RepeatingDrawable(parent.getContext().getDrawable(R.drawable.piano_roll_column), RepeatingDrawable.HORIZONTAL));

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewHolderSet.add(holder);
        ViewGroup.LayoutParams params = holder.notePane.getLayoutParams();
        params.width = rollWidth;
        holder.notePane.setLayoutParams(params);

        holder.notePane.removeAllViews();
        for(PianoRollNoteView n : pianoRollNotes) {
            if(n.containerIndex == position) {
                holder.notePane.addView(n, n.layoutParams);
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

    public void setPianoRollNotes(List<PianoRollNoteView> notes) {
        pianoRollNotes = notes;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void updateRollLength(int width) {
        this.rollWidth = width;
        for(ViewHolder holder : viewHolderSet) {
            ViewGroup.LayoutParams params = holder.notePane.getLayoutParams();
            params.width = width;
            holder.notePane.setLayoutParams(params);
        }
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

            Rect r = new Rect();
            for(PianoRollNoteView n : pianoRollNotes) {
                if(n.containerIndex == containerIndex) {
                    n.getLocalVisibleRect(r);
                    holder.notePane.offsetDescendantRectToMyCoords(n, r);
                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        holder.notePane.removeView(n);
                        pianoRollController.onRemovePianoRollNote(n);
                        pianoRollNotes.remove(n);
                        return;
                    }
                }
            }

            PianoRollNoteView note = new PianoRollNoteView(holder.notePane.getContext());
            pianoRollController.onCreatePianoRollNote(note, containerIndex, e.getX(), e.getY());  // set layout params
            pianoRollNotes.add(note);
            holder.notePane.addView(note, note.layoutParams);
        }
    }

}
