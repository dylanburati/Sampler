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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.RepeatingBarDrawable;
import libre.sampler.views.VisualNote;

public class PianoRollAdapter extends RecyclerView.Adapter<PianoRollAdapter.ViewHolder> {
    public static final int SPAN_COUNT = 8;

    private List<VisualNote> pianoRollNotes = Collections.emptyList();
    private ProjectPatternsFragment controller;
    private List<ViewHolder> viewHolderList;
    private int rollWidth;
    private boolean enabled;

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
        this.viewHolderList = new ArrayList<>(SPAN_COUNT);
        for(int i = 0; i < SPAN_COUNT; i++) {
            viewHolderList.add(null);
        }
        enabled = false;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_piano_roll_column, parent, false);
        v.findViewById(R.id.piano_roll_column).setBackground(
                new RepeatingBarDrawable(parent.getContext().getDrawable(R.drawable.piano_roll_column), RepeatingBarDrawable.HORIZONTAL));

        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewHolderList.set(position, holder);
        ViewGroup.LayoutParams params = holder.notePane.getLayoutParams();
        params.width = rollWidth;
        holder.notePane.setLayoutParams(params);

        holder.notePane.removeAllViews();
        for(VisualNote n : pianoRollNotes) {
            if(n.containerIndex == position) {
                displayNote(holder, n, controller.getSelectedNotes().contains(n));
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

    public List<VisualNote> getPianoRollNotes() {
        return pianoRollNotes;
    }

    private void displayNote(ViewHolder holder, VisualNote note, boolean isSelected) {
        double tickWidth = controller.getTickWidth();
        float keyHeight = controller.getKeyHeight();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (note.lengthTicks * tickWidth), (int) keyHeight);
        layoutParams.leftMargin = (int) (note.startTicks * tickWidth);
        layoutParams.topMargin = (int) (note.keyIndex * keyHeight);
        View view = new View(holder.notePane.getContext());
        view.setBackgroundColor(Color.WHITE);
        view.setBackgroundTintList(holder.notePane.getResources().getColorStateList(R.color.piano_roll_note));
        view.setTag(note.tag);
        view.setActivated(isSelected);

        holder.notePane.addView(view, layoutParams);
    }

    public void updateNote(VisualNote note, boolean isSelected) {
        ViewHolder holder = viewHolderList.get(note.containerIndex);
        if(holder == null) {
            return;
        }
        if(pianoRollNotes.contains(note)) {
            View view = holder.notePane.findViewWithTag(note.tag);
            if(view != null) {
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) view.getLayoutParams();
                layoutParams.leftMargin = (int) (note.startTicks * controller.getTickWidth());
                layoutParams.topMargin = (int) (note.keyIndex * controller.getKeyHeight());
                layoutParams.width = (int) (note.lengthTicks * controller.getTickWidth());
                view.setLayoutParams(layoutParams);
                view.setActivated(isSelected);
            }
        }
    }

    public void removeNote(VisualNote note) {
        ViewHolder holder = viewHolderList.get(note.containerIndex);
        if(holder == null) {
            return;
        }
        View view = holder.notePane.findViewWithTag(note.tag);
        holder.notePane.removeView(view);
        pianoRollNotes.remove(note);
    }

    public void addNote(VisualNote note, boolean isSelected) {
        ViewHolder holder = viewHolderList.get(note.containerIndex);
        if(holder == null) {
            return;
        }
        pianoRollNotes.add(note);
        displayNote(holder, note, isSelected);
    }

    public void setPianoRollNotes(List<VisualNote> notes) {
        pianoRollNotes = notes;
        enabled = true;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void updateAllNotes() {
        Set<VisualNote> selectedNotes = controller.getSelectedNotes();
        for(VisualNote note : pianoRollNotes) {
            updateNote(note, selectedNotes.contains(note));
        }
    }

    public void updateRollLength(int width) {
        this.rollWidth = width;
        for(ViewHolder holder : viewHolderList) {
            if(holder != null) {
                ViewGroup.LayoutParams params = holder.notePane.getLayoutParams();
                params.width = width;
                holder.notePane.setLayoutParams(params);
            }
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
        public boolean onSingleTapUp(MotionEvent e) {
            if(!enabled) {
                return false;
            }
            int containerIndex = holder.getAdapterPosition();

            Rect r = new Rect();
            for(VisualNote n : pianoRollNotes) {
                if(n.containerIndex == containerIndex) {
                    View view = holder.notePane.findViewWithTag(n.tag);
                    view.getLocalVisibleRect(r);
                    holder.notePane.offsetDescendantRectToMyCoords(view, r);
                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        boolean isSelected = controller.onAdapterSelectNote(n);
                        view.setActivated(isSelected);
                    }
                }
            }

            controller.dispatchPianoRollTap(containerIndex, e.getX(), e.getY());
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if(!enabled) {
                return;
            }
            int containerIndex = holder.getAdapterPosition();

            Rect r = new Rect();
            for(VisualNote n : pianoRollNotes) {
                if(n.containerIndex == containerIndex) {
                    View view = holder.notePane.findViewWithTag(n.tag);
                    view.getLocalVisibleRect(r);
                    holder.notePane.offsetDescendantRectToMyCoords(view, r);
                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        holder.notePane.removeView(view);
                        controller.onAdapterRemoveNote(n);
                        pianoRollNotes.remove(n);
                        controller.patternEditEventSource.dispatch(AppConstants.PIANO_ROLL_NOTES);
                        return;
                    }
                }
            }

            controller.onAdapterCreateNote(containerIndex, e.getX(), e.getY());
        }
    }

}
