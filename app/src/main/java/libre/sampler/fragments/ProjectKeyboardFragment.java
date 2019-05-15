package libre.sampler.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.ProjectActivity;
import libre.sampler.R;
import libre.sampler.adapters.PianoAdapter;
import libre.sampler.models.NoteEvent;
import libre.sampler.publishers.NoteEventSource;

public class ProjectKeyboardFragment extends Fragment {
    private RecyclerView pianoContainer;
    private PianoAdapter pianoAdapter;
    private final Map<Pair<Long, Integer>, KeyData> noteQueue = new HashMap<>();
    private NoteEventSource noteEventSource;

    private class KeyData {
        public View keyView;
        public int keyNum;

        public KeyData(View keyView, int keyNum) {
            this.keyView = keyView;
            this.keyNum = keyNum;
        }
    }

    public ProjectKeyboardFragment() {
    }

    private KeyData resolveKeyNum(View octaveContainer, float x, float y) {
        if(octaveContainer == null) {
            return new KeyData(null, -1);
        }
        int octave = pianoContainer.getChildAdapterPosition(octaveContainer);
        if(octave == RecyclerView.NO_POSITION) {
            return new KeyData(null, -1);
        }
        ViewGroup vg = (ViewGroup) octaveContainer;
        int[] resIds = new int[]{R.id.piano_c_sharp, R.id.piano_d_sharp, R.id.piano_f_sharp,
                R.id.piano_g_sharp, R.id.piano_a_sharp, R.id.piano_c, R.id.piano_d,
                R.id.piano_e, R.id.piano_f, R.id.piano_g, R.id.piano_a, R.id.piano_b};
        int[] offsets = new int[]{1, 3, 6, 8, 10, 0, 2, 4, 5, 7, 9, 11};

        int keyNum = -1;
        View keyView = null;
        for(int i = 0; i < 12; i++) {
            keyView = vg.findViewById(resIds[i]);
            Rect r = new Rect();
            keyView.getLocalVisibleRect(r);
            vg.offsetDescendantRectToMyCoords(keyView, r);
            pianoContainer.offsetDescendantRectToMyCoords(octaveContainer, r);

            if(r.contains((int) x, (int) y)) {
                keyNum = (octave + 2) * 12 + offsets[i];
                break;
            }
        }
        return new KeyData(keyView, keyNum);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_keyboard, container, false);
        pianoContainer = rootView.findViewById(R.id.piano_container);
        noteEventSource = ((ProjectActivity) getActivity()).noteEventSource;

        noteEventSource.add("logger", new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                Log.d("ProjectKeyboardFragment", String.format("noteEvent: action=%d keynum=%d velocity=%d", noteEvent.action, noteEvent.keyNum, noteEvent.velocity));
            }
        });
        
        pianoContainer.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                float scrollBarHeight = getResources().getDimension(R.dimen.caption) + getResources().getDimension(R.dimen.margin2) * 2;
                if((e.getAction() == MotionEvent.ACTION_DOWN) && e.getY() > scrollBarHeight) {
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int pointerCount = e.getPointerCount();
                for(int i = 0; i < pointerCount; i++) {
                    View octaveContainer = rv.findChildViewUnder(e.getX(i), e.getY(i));
                    int eventAction = e.getActionMasked();
                    Pair<Long, Integer> eventId = new Pair<>(e.getDownTime(), e.getPointerId(i));
                    KeyData keyData = resolveKeyNum(octaveContainer, e.getX(i), e.getY(i));

                    if(eventAction == MotionEvent.ACTION_CANCEL || eventAction == MotionEvent.ACTION_UP
                            || (eventAction == MotionEvent.ACTION_POINTER_UP && e.getActionIndex() == i)) {
                        KeyData previous = noteQueue.get(eventId);
                        if(previous != null) {
                            NoteEvent prevEndEvent = new NoteEvent(NoteEvent.NOTE_OFF, previous.keyNum, 100, eventId);
                            noteEventSource.dispatch(prevEndEvent);
                            noteQueue.remove(eventId);
                            previous.keyView.setActivated(false);
                        }
                        continue;
                    }

                    if(eventAction == MotionEvent.ACTION_DOWN || eventAction == MotionEvent.ACTION_POINTER_DOWN) {
                        if(keyData.keyNum != -1 && !noteQueue.containsKey(eventId)) {
                            NoteEvent noteEvent = new NoteEvent(NoteEvent.NOTE_ON, keyData.keyNum, 100, eventId);
                            noteEventSource.dispatch(noteEvent);
                            noteQueue.put(eventId, keyData);
                            keyData.keyView.setActivated(true);
                        }
                    } else if(eventAction == MotionEvent.ACTION_MOVE) {
                        KeyData previous = noteQueue.get(eventId);
                        if(previous != null && previous.keyNum != keyData.keyNum) {
                            NoteEvent prevEndEvent = new NoteEvent(NoteEvent.NOTE_OFF, previous.keyNum, 100, eventId);
                            noteEventSource.dispatch(prevEndEvent);
                            previous.keyView.setActivated(false);
                            if(keyData.keyNum != -1) {
                                NoteEvent noteEvent = new NoteEvent(NoteEvent.NOTE_ON, keyData.keyNum, 100, eventId);
                                noteEventSource.dispatch(noteEvent);
                                noteQueue.put(eventId, keyData);
                                keyData.keyView.setActivated(true);
                            }
                        }
                    }
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
        pianoAdapter = new PianoAdapter();
        pianoContainer.setAdapter(pianoAdapter);
        return rootView;
    }
}
