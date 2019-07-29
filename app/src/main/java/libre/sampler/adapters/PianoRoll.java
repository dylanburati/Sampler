package libre.sampler.adapters;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.view.GestureDetectorCompat;
import libre.sampler.R;
import libre.sampler.fragments.ProjectPatternsFragment;
import libre.sampler.utils.RepeatingBarDrawable;
import libre.sampler.views.VisualNote;

public class PianoRoll {
    public static final int ROW_COUNT = 8;

    public LinearLayout rootView;
    private Map<Long, View> viewPool = new HashMap<>();

    // must be set to a pattern's derived List<VisualNote> instance before anything is added
    private List<VisualNote> pianoRollNotes = Collections.emptyList();
    private ProjectPatternsFragment controller;
    private int rollWidth;
    private boolean enabled;

    private List<RelativeLayout> noteContainerList;

    public PianoRoll(ProjectPatternsFragment controller, LinearLayout rootView) {
        this.rootView = rootView;
        this.controller = controller;
        this.noteContainerList = new ArrayList<>(ROW_COUNT);
        for(int i = 0; i < ROW_COUNT; i++) {
            RelativeLayout notePane = rootView.getChildAt(i).findViewById(R.id.note_pane);
            notePane.setBackground(
                    new RepeatingBarDrawable(rootView.getContext().getDrawable(R.drawable.piano_roll_row), RepeatingBarDrawable.HORIZONTAL));
            noteContainerList.add(notePane);
        }
        bindAllRows();
        enabled = false;
    }

    private void bindAllRows() {
        for(int i = 0; i < noteContainerList.size(); i++) {
            bindRow(noteContainerList.get(i), i);
        }
    }

    private void bindRow(@NonNull RelativeLayout notePane, int position) {
        int containerIndex = position;

        notePane.getLayoutParams().width = rollWidth;
        notePane.requestLayout();
        
        notePane.removeAllViews();
        for(VisualNote n : pianoRollNotes) {
            if(n.containerIndex == containerIndex) {
                displayNote(notePane, n, controller.getSelectedNotes().contains(n));
            }
        }

        final GestureDetectorCompat detector = new GestureDetectorCompat(notePane.getContext(),
                new MyGestureListener(notePane));
        notePane.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return detector.onTouchEvent(event);
            }
        });
    }

    public List<VisualNote> getPianoRollNotes() {
        return pianoRollNotes;
    }

    private void displayNote(RelativeLayout notePane, VisualNote note, boolean isSelected) {
        double tickWidth = controller.getTickWidth();
        float keyHeight = controller.getKeyHeight();
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (note.lengthTicks * tickWidth), (int) keyHeight);
        layoutParams.leftMargin = (int) (note.startTicks * tickWidth);
        layoutParams.topMargin = (int) (note.keyIndex * keyHeight);
        View view = viewPool.remove(note.tag);
        if(view == null) {
            view = new View(notePane.getContext());
            view.setBackgroundColor(Color.WHITE);
            view.setAlpha(0.8f);
            view.setBackgroundTintList(notePane.getResources().getColorStateList(R.color.piano_roll_note));
            view.setTag(note.tag);
            view.setActivated(isSelected);
            notePane.addView(view, layoutParams);
        } else {
            // view still attached
            view.setActivated(isSelected);
            view.setLayoutParams(layoutParams);
        }
    }

    public void updateNote(VisualNote note, boolean isSelected) {
        RelativeLayout notePane = noteContainerList.get(note.containerIndex);
        if(notePane == null) {
            return;
        }
        if(pianoRollNotes.contains(note)) {
            View view = notePane.findViewWithTag(note.tag);
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

    public void removeNote(VisualNote note, boolean willAddBack) {
        RelativeLayout notePane = noteContainerList.get(note.containerIndex);
        if(notePane == null) {
            return;
        }
        View view = notePane.findViewWithTag(note.tag);
        if(willAddBack) {
            viewPool.put(note.tag, view);
        } else {
            notePane.removeView(view);
        }
        pianoRollNotes.remove(note);
    }

    public void addNote(VisualNote note, boolean isSelected) {
        RelativeLayout notePane = noteContainerList.get(note.containerIndex);
        if(notePane == null) {
            return;
        }
        pianoRollNotes.add(note);
        displayNote(notePane, note, isSelected);
    }

    public void setPianoRollNotes(List<VisualNote> notes) {
        pianoRollNotes = notes;
        enabled = true;
        bindAllRows();
    }

    public void updateAllNotes() {
        Set<VisualNote> selectedNotes = controller.getSelectedNotes();
        for(VisualNote note : pianoRollNotes) {
            updateNote(note, selectedNotes.contains(note));
        }
    }

    public void updateRollLength(int width) {
        this.rollWidth = width;
        for(RelativeLayout notePane : noteContainerList) {
            notePane.getLayoutParams().width = width;
            notePane.requestLayout();
        }
    }

    private Rect tmpRect = new Rect();
    private VisualNote getNoteUnder(int containerIdx, float x, float y, View[] outView) {
        RelativeLayout notePane = noteContainerList.get(containerIdx);
        int keyIndex = (int) (y / controller.getKeyHeight());

        ListIterator<VisualNote> reversed = pianoRollNotes.listIterator(pianoRollNotes.size());
        while(reversed.hasPrevious()) {
            VisualNote n = reversed.previous();
            if(n.containerIndex == containerIdx && n.keyIndex == keyIndex) {
                View view = notePane.findViewWithTag(n.tag);
                view.getLocalVisibleRect(tmpRect);
                notePane.offsetDescendantRectToMyCoords(view, tmpRect);
                if(tmpRect.contains((int) x, (int) y)) {
                    if(outView != null) {
                        outView[0] = view;
                    }
                    return n;
                }
            }
        }
        return null;
    }

    public void postDelayed(Runnable action, long delayMillis) {
        noteContainerList.get(0).postDelayed(action, delayMillis);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private RelativeLayout notePane;
        private View[] tmpViewUnder = new View[1];

        public MyGestureListener(RelativeLayout notePane) {
            this.notePane = notePane;
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
            int containerIndex = noteContainerList.indexOf(notePane);

            VisualNote toSelect = getNoteUnder(containerIndex, e.getX(), e.getY(), tmpViewUnder);
            if(toSelect != null) {
                boolean isSelected = controller.onAdapterSelectNote(toSelect);
                tmpViewUnder[0].setActivated(isSelected);
            }

            controller.dispatchPianoRollTap(containerIndex, e.getX(), e.getY());
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            if(!enabled) {
                return;
            }
            int containerIndex = noteContainerList.indexOf(notePane);

            VisualNote toRemove = getNoteUnder(containerIndex, e.getX(), e.getY(), null);

            if(toRemove != null) {
                controller.removeFromPianoRollPattern(toRemove, false);
            } else {
                controller.onAdapterCreateNote(containerIndex, e.getX(), e.getY());
            }
        }
    }
}
