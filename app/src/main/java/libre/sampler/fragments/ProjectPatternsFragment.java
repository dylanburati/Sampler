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
import libre.sampler.R;
import libre.sampler.adapters.PianoAdapter;
import libre.sampler.models.NoteEvent;
import libre.sampler.publishers.NoteEventSource;

public class ProjectPatternsFragment extends Fragment {
    private RecyclerView pianoContainer;
    private PianoAdapter pianoAdapter;
    private final Map<Pair<Long, Integer>, NoteEvent> noteQueue = new HashMap<>();
    public NoteEventSource noteEventSource = new NoteEventSource();

    public ProjectPatternsFragment() {
    }

    private int resolveKeyNum(View octaveContainer, float x, float y) {
        if(octaveContainer == null) {
            return -1;
        }
        int octave = pianoContainer.getChildAdapterPosition(octaveContainer);
        if(octave == RecyclerView.NO_POSITION) {
            return -1;
        }
        ViewGroup vg = (ViewGroup) octaveContainer;
        int[] resIds = new int[]{R.id.piano_c_sharp, R.id.piano_d_sharp, R.id.piano_f_sharp,
                R.id.piano_g_sharp, R.id.piano_a_sharp, R.id.piano_c, R.id.piano_d,
                R.id.piano_e, R.id.piano_f, R.id.piano_g, R.id.piano_a, R.id.piano_b};
        int[] offsets = new int[]{2, 4, 7, 9, 11, 1, 3, 5, 6, 8, 10, 12};
        int keyNum = -1;
        for(int i = 0; i < 12; i++) {
            View keyView = vg.findViewById(resIds[i]);
            Rect r = new Rect();
            keyView.getLocalVisibleRect(r);
            vg.offsetDescendantRectToMyCoords(keyView, r);
            pianoContainer.offsetDescendantRectToMyCoords(octaveContainer, r);

            if(r.contains((int) x, (int) y)) {
                keyNum = octave * 12 + offsets[i];
                break;
            }
        }
        return keyNum;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        pianoContainer = rootView.findViewById(R.id.piano_container);

        noteEventSource.add(new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                Log.d("ProjectPatternsFragment", String.format("noteEvent: keynum=%d action=%d", noteEvent.keyNum, noteEvent.action));
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
                    Pair<Long, Integer> eventKey = new Pair<>(e.getDownTime(), e.getPointerId(i));

                    if(eventAction == MotionEvent.ACTION_CANCEL || eventAction == MotionEvent.ACTION_UP
                            || (eventAction == MotionEvent.ACTION_POINTER_UP && e.getActionIndex() == i)) {
                        NoteEvent previous = noteQueue.get(eventKey);
                        if(previous != null) {
                            NoteEvent prevEndEvent = new NoteEvent(previous.keyNum, NoteEvent.ACTION_END);
                            noteEventSource.dispatch(prevEndEvent);
                            noteQueue.remove(eventKey);
                        }
                        continue;
                    }

                    int keyNum = resolveKeyNum(octaveContainer, e.getX(i), e.getY(i));
                    if(eventAction == MotionEvent.ACTION_DOWN || eventAction == MotionEvent.ACTION_POINTER_DOWN) {
                        if(!noteQueue.containsKey(eventKey)) {
                            NoteEvent noteEvent = new NoteEvent(keyNum, NoteEvent.ACTION_BEGIN);
                            noteQueue.put(eventKey, noteEvent);
                            noteEventSource.dispatch(noteEvent);
                        }
                    } else if(eventAction == MotionEvent.ACTION_MOVE) {
                        NoteEvent previous = noteQueue.get(eventKey);
                        if(previous != null && previous.keyNum != keyNum) {
                            NoteEvent prevEndEvent = new NoteEvent(previous.keyNum, NoteEvent.ACTION_END);
                            noteEventSource.dispatch(prevEndEvent);
                            NoteEvent noteEvent = new NoteEvent(keyNum, NoteEvent.ACTION_BEGIN);
                            noteQueue.put(eventKey, noteEvent);
                            noteEventSource.dispatch(noteEvent);
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
