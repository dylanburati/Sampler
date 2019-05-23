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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.utils.RepeatingDrawable;
import libre.sampler.views.VisualNote;

public class PianoRollAdapter extends RecyclerView.Adapter<PianoRollAdapter.ViewHolder> {
    public static final int SPAN_COUNT = 8;

    public List<VisualNote> pianoRollNotes;
    private ProjectPatternsFragment controller;
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

    public PianoRollAdapter(ProjectPatternsFragment controller) {
        this.controller = controller;
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
        for(VisualNote n : pianoRollNotes) {
            if(n.containerIndex == position) {
                displayNote(holder, n);
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

    public void displayNote(ViewHolder holder, VisualNote note) {
        double tickWidth = controller.getTickWidth();
        float keyHeight = controller.getKeyHeight();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (note.lengthTicks * tickWidth), (int) keyHeight);
        layoutParams.leftMargin = (int) (note.startTicks * tickWidth);
        layoutParams.topMargin = (int) (note.keyIndex * keyHeight);
        View view = new View(holder.notePane.getContext());
        view.setBackgroundColor(Color.WHITE);
        view.setTag(note.tag);

        holder.notePane.addView(view, layoutParams);
    }

    public void setPianoRollNotes(List<VisualNote> notes) {
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
            for(VisualNote n : pianoRollNotes) {
                if(n.containerIndex == containerIndex) {
                    View view = holder.notePane.findViewWithTag(n.tag);
                    view.getLocalVisibleRect(r);
                    holder.notePane.offsetDescendantRectToMyCoords(view, r);
                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        holder.notePane.removeView(view);
                        controller.onRemovePianoRollNote(n);
                        pianoRollNotes.remove(n);
                        return;
                    }
                }
            }

            VisualNote note = new VisualNote(containerIndex);
            controller.onCreatePianoRollNote(note, containerIndex, e.getX(), e.getY());
            pianoRollNotes.add(note);
            displayNote(holder, note);
        }
    }

}
