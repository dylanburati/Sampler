package libre.sampler.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.PianoAdapter;
import libre.sampler.models.NoteEvent;
import libre.sampler.models.ProjectViewModel;
import libre.sampler.utils.NoteId;

public class ProjectKeyboardFragment extends Fragment {
    private RecyclerView pianoContainer;
    private PianoAdapter pianoAdapter;
    private final Map<Pair<Long, Integer>, KeyData> noteQueue = new HashMap<>();
    private ProjectViewModel viewModel;

    private class KeyData {
        public View keyView;
        public int keyNum;
        public float yFraction;

        public KeyData() {
            this.keyNum = -1;
        }

        public KeyData(View keyView, int keyNum, float yFraction) {
            this.keyView = keyView;
            this.keyNum = keyNum;
            this.yFraction = yFraction;
        }
    }

    private final int[] KEY_IDS = new int[]{R.id.piano_c_sharp, R.id.piano_d_sharp, R.id.piano_f_sharp,
            R.id.piano_g_sharp, R.id.piano_a_sharp, R.id.piano_c, R.id.piano_d,
            R.id.piano_e, R.id.piano_f, R.id.piano_g, R.id.piano_a, R.id.piano_b};
    private final int[] KEY_OFFSETS = new int[]{1, 3, 6, 8, 10, 0, 2, 4, 5, 7, 9, 11};
    private float scrollBarHeight;
    private float keyboardHeight;

    private KeyData resolveKeyNum(View octaveContainer, float x, float y) {
        if(octaveContainer == null) {
            return new KeyData();
        }
        int octave = pianoContainer.getChildAdapterPosition(octaveContainer);
        if(octave == RecyclerView.NO_POSITION) {
            return new KeyData();
        }
        ViewGroup vg = (ViewGroup) octaveContainer;

        for(int i = 0; i < KEY_IDS.length; i++) {
            View keyView = vg.findViewById(KEY_IDS[i]);
            Rect r = new Rect();
            keyView.getLocalVisibleRect(r);
            vg.offsetDescendantRectToMyCoords(keyView, r);
            pianoContainer.offsetDescendantRectToMyCoords(octaveContainer, r);

            if(r.contains((int) x, (int) y)) {
                int keyNum = (octave + 2) * 12 + KEY_OFFSETS[i];
                float yFraction = (y - r.top) / r.height();
                return new KeyData(keyView, keyNum, yFraction);
            }
        }
        return new KeyData();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_keyboard, container, false);
        pianoContainer = rootView.findViewById(R.id.piano_container);
        viewModel = ViewModelProviders.of(getActivity()).get(ProjectViewModel.class);

        scrollBarHeight = getResources().getDimension(R.dimen.text_caption) + getResources().getDimension(R.dimen.margin2) * 3;
        keyboardHeight = getResources().getDimension(R.dimen.piano_height);
        pianoContainer.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if((e.getAction() == MotionEvent.ACTION_DOWN) && e.getY() > scrollBarHeight) {
                    return true;
                }
                return false;
            }

            private void sendNoteOn(NoteId eventId, KeyData keyData, float pressure, boolean enqueue) {
                NoteEvent noteEvent = new NoteEvent(
                        NoteEvent.NOTE_ON,
                        viewModel.getKeyboardInstrument(),
                        keyData.keyNum,
                        viewModel.getProject().getTouchVelocitySource().getVelocity(keyData.yFraction, pressure),
                        eventId
                );
                viewModel.noteEventSource.dispatch(noteEvent);
                if(enqueue) noteQueue.put(eventId, keyData);
                keyData.keyView.setActivated(true);
            }

            private void sendNoteOff(NoteId eventId, KeyData keyData, boolean dequeue) {
                NoteEvent prevEndEvent = new NoteEvent(
                        NoteEvent.NOTE_OFF,
                        viewModel.getKeyboardInstrument(),
                        keyData.keyNum,
                        0,
                        eventId
                );
                viewModel.noteEventSource.dispatch(prevEndEvent);
                if(dequeue) noteQueue.remove(eventId);
                keyData.keyView.setActivated(false);
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                int pointerCount = e.getPointerCount();
                for(int i = 0; i < pointerCount; i++) {
                    View octaveContainer = rv.findChildViewUnder(e.getX(i), e.getY(i));
                    int eventAction = e.getActionMasked();
                    NoteId eventId = NoteId.createForKeyboard(e.getDownTime(), e.getPointerId(i));
                    KeyData keyData = resolveKeyNum(octaveContainer, e.getX(i), e.getY(i));

                    if(eventAction == MotionEvent.ACTION_CANCEL || eventAction == MotionEvent.ACTION_UP
                            || (eventAction == MotionEvent.ACTION_POINTER_UP && e.getActionIndex() == i)) {
                        KeyData previous = noteQueue.get(eventId);
                        if(previous != null && previous.keyNum != -1) {
                            sendNoteOff(eventId, previous, true);
                        }
                    } else if(eventAction == MotionEvent.ACTION_DOWN || eventAction == MotionEvent.ACTION_POINTER_DOWN) {
                        if(keyData.keyNum != -1 && !noteQueue.containsKey(eventId)) {
                            sendNoteOn(eventId, keyData, e.getPressure(i), true);
                        }
                    } else if(eventAction == MotionEvent.ACTION_MOVE) {
                        KeyData previous = noteQueue.get(eventId);
                        if(previous != null && previous.keyNum != keyData.keyNum) {
                            if(previous.keyNum != -1) {
                                // moved off of a key
                                sendNoteOff(eventId, previous, false);
                            }

                            if(keyData.keyNum != -1) {
                                // moved onto a key
                                sendNoteOn(eventId, keyData, e.getPressure(i), false);
                            }
                            noteQueue.put(eventId, keyData);  // new location saved as last regardless
                        }
                    }
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
        pianoAdapter = new PianoAdapter(getResources());
        pianoContainer.setAdapter(pianoAdapter);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        pianoContainer.setAdapter(null);
        super.onDestroyView();
        this.pianoContainer = null;
    }
}
