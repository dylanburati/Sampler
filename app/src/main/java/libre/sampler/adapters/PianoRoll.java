package libre.sampler.adapters;

import android.content.res.Resources;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
import libre.sampler.models.NoteEvent;
import libre.sampler.models.ScheduledNoteEvent;
import libre.sampler.utils.AppConstants;
import libre.sampler.utils.MusicTime;
import libre.sampler.utils.NoteId;
import libre.sampler.utils.RepeatingBarDrawable;
import libre.sampler.views.VisualNote;

public class PianoRoll {
    public static final int ROW_COUNT = 8;

    private LinearLayout rootView;
    private Map<Long, View> viewPool = new HashMap<>();

    // must be set to a pattern's derived List<VisualNote> instance before anything is added
    private List<VisualNote> pianoRollNotes = Collections.emptyList();
    private ProjectPatternsFragment controller;

    private boolean enabled;
    private float keyHeight;
    private double tickWidth;
    private int rollWidth;
    private long rollTicks;
    private MusicTime inputNoteLength = new MusicTime(0, 4, 0);
    private MusicTime snap = new MusicTime(0, 1, 0);
    private int velocity = 100;

    private List<RelativeLayout> noteContainerList;

    public PianoRoll(ProjectPatternsFragment controller, LinearLayout rootView) {
        this.rootView = rootView;
        this.controller = controller;
        this.noteContainerList = new ArrayList<>(ROW_COUNT);

        Resources res = rootView.getContext().getResources();
        this.keyHeight = res.getDimension(R.dimen.piano_roll_colheight) / 12.0f;
        this.tickWidth = res.getDimension(R.dimen.piano_roll_barwidth) / 1.0 / MusicTime.TICKS_PER_BAR;
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
            if(n.getContainerIndex() == containerIndex) {
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
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                (int) (note.getLengthTicks() * tickWidth), (int) keyHeight);
        layoutParams.leftMargin = (int) (note.getStartTicks() * tickWidth);
        layoutParams.topMargin = (int) (note.getKeyIndex() * keyHeight);
        View view = viewPool.remove(note.tag);
        if(view != null && view.getParent() == notePane) {
            // view attached in correct row
            view.setActivated(isSelected);
            view.setLayoutParams(layoutParams);
        } else {
            if(view != null) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            view = new View(notePane.getContext());
            view.setBackground(notePane.getResources().getDrawable(R.drawable.piano_roll_note));
            view.setAlpha(0.8f);
            view.setBackgroundTintList(notePane.getResources().getColorStateList(R.color.piano_roll_note));
            view.setTag(note.tag);
            view.setActivated(isSelected);
            notePane.addView(view, layoutParams);
        }
    }

    public void setNoteActivated(VisualNote note, boolean isSelected) {
        RelativeLayout notePane = noteContainerList.get(note.getContainerIndex());
        if(notePane == null) {
            return;
        }
        View view = notePane.findViewWithTag(note.tag);
        if(view != null) {
            view.setActivated(isSelected);
        }
    }

    public void removeNote(VisualNote note, boolean willAddBack) {
        RelativeLayout notePane = noteContainerList.get(note.getContainerIndex());
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
        RelativeLayout notePane = noteContainerList.get(note.getContainerIndex());
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

    public void deselectAllNotes() {
        for(RelativeLayout notePane : noteContainerList) {
            notePane.dispatchSetActivated(false);
        }
    }

    public void selectAllNotes() {
        for(RelativeLayout notePane : noteContainerList) {
            notePane.dispatchSetActivated(true);
        }
    }

    public void syncSelectedNotes() {
        Set<VisualNote> selectedNotes = controller.getSelectedNotes();
        for(VisualNote note : pianoRollNotes) {
            setNoteActivated(note, selectedNotes.contains(note));
        }
    }

    public void updateRollTicks(long ticks) {
        this.rollTicks = ticks;
        this.rollWidth = (int) (ticks * tickWidth);
        for(RelativeLayout notePane : noteContainerList) {
            notePane.getLayoutParams().width = rollWidth;
            notePane.requestLayout();
        }
    }

    private final Rect tmpRect = new Rect();
    private VisualNote getNoteUnder(int containerIdx, float x, float y, View[] outView) {
        RelativeLayout notePane = noteContainerList.get(containerIdx);
        int keyIndex = (int) (y / keyHeight);

        ListIterator<VisualNote> reversed = pianoRollNotes.listIterator(pianoRollNotes.size());
        while(reversed.hasPrevious()) {
            VisualNote n = reversed.previous();
            if(n.getContainerIndex() == containerIdx && n.getKeyIndex() == keyIndex) {
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

    public MusicTime getInputNoteLength() {
        return inputNoteLength;
    }

    public void setInputNoteLength(MusicTime inputNoteLength) {
        this.inputNoteLength.set(inputNoteLength);
    }
    
    public void setInputNoteLength(long ticks) {
        this.inputNoteLength.setTicks(ticks);
    }

    public MusicTime getSnapLength() {
        return snap;
    }

    public void setSnapLength(MusicTime snap) {
        this.snap.set(snap);
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public double getTickWidth() {
        return tickWidth;
    }

    public float getKeyHeight() {
        return keyHeight;
    }

    public int getRollWidth() {
        return rollWidth;
    }

    private final Rect tmpRect2 = new Rect();
    public PianoRoll.MusicRect getVisibleRect() {
        rootView.getLocalVisibleRect(tmpRect2);
        MusicTime left = new MusicTime((long) (tmpRect2.left / 1.0 / tickWidth));
        MusicTime right = new MusicTime((long) (tmpRect2.right / 1.0 / tickWidth));
        int top = AppConstants.PIANO_ROLL_TOP_KEYNUM - (int) Math.floor(tmpRect2.top / keyHeight);
        int bottom = AppConstants.PIANO_ROLL_TOP_KEYNUM - (int) Math.ceil(tmpRect2.bottom / keyHeight) + 1;

        return new MusicRect(left, top, right, bottom);
    }
    
    public int resolveKeyNum(int containerIndex, float y) {
        int keyIndex = (int) (y / keyHeight);
        return (9 - containerIndex) * 12 + (11 - keyIndex);
    }
    
    private VisualNote createVisualNote(int containerIndex, float x, float y) {
        double tickWidth = getTickWidth();
        double inputTicks = x / tickWidth;
        long snapTicks = snap.getTicks();

        long startTicks = Math.round(inputTicks / 1.0 / snapTicks - 0.3) * snapTicks;

        long maxTicks = rollTicks - startTicks;
        long lengthTicks = inputNoteLength.getTicks();
        if(lengthTicks > maxTicks) {
            lengthTicks = (long) Math.floor(maxTicks / 1.0 / snapTicks) * snapTicks;
        }

        int keyNum = resolveKeyNum(containerIndex, y);
        long baseId = NoteId.createForScheduledNoteEvent(System.currentTimeMillis(), 0);
        return new VisualNote(
                new ScheduledNoteEvent(startTicks, NoteEvent.NOTE_ON,
                        controller.getPianoRollInstrument(), keyNum, velocity, baseId),
                new ScheduledNoteEvent(startTicks + lengthTicks, NoteEvent.NOTE_OFF,
                        controller.getPianoRollInstrument(), keyNum, velocity, baseId)
        );
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
                boolean isSelected = controller.toggleNoteSelected(toSelect);
                tmpViewUnder[0].setActivated(isSelected);
            }

            int keyNum = resolveKeyNum(containerIndex, e.getY());
            MusicTime xTime = new MusicTime((long) (e.getX() / tickWidth));
            controller.dispatchPianoRollTap(xTime, keyNum);
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
                VisualNote visualNote = createVisualNote(containerIndex, e.getX(), e.getY());
                controller.addToPianoRollPattern(visualNote);
            }
        }
    }

    public class MusicRect {
        public MusicTime left;
        public MusicTime right;
        public int top;
        public int bottom;
        public MusicRect(MusicTime left, int top, MusicTime right, int bottom) {
            this.left = left;
            this.top = top;
            this.right = right;
            this.bottom = bottom;
        }
    }
}
