package libre.sampler.fragments;

import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.adapters.PianoAdapter;
import libre.sampler.models.NoteEvent;

public class ProjectPatternsFragment extends Fragment {
    private RecyclerView pianoContainer;
    private PianoAdapter pianoAdapter;

    public ProjectPatternsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_project_patterns, container, false);
        pianoContainer = rootView.findViewById(R.id.piano_container);

        final Consumer<NoteEvent> clickPostHook = new Consumer<NoteEvent>() {
            @Override
            public void accept(NoteEvent noteEvent) {
                Log.d("StatefulTouchListener", String.format("noteEvent: keynum=%d action=%d", noteEvent.keyNum, noteEvent.action));
            }
        };
        pianoContainer.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                if(e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_POINTER_UP) {
                    Log.d("onItemTouchListener","actionUp");
                }
                float scrollBarHeight = getResources().getDimension(R.dimen.caption) + getResources().getDimension(R.dimen.margin2) * 2;
                if((e.getAction() == MotionEvent.ACTION_DOWN) && e.getY() > scrollBarHeight) {
                    return true;
                }
                return false;
            }

            @Override
            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                View octaveContainer = rv.findChildViewUnder(e.getX(), e.getY());
                if(octaveContainer == null) return;

                int octave = rv.getChildAdapterPosition(octaveContainer);
                if(octave == RecyclerView.NO_POSITION) return;

                ViewGroup vg = (ViewGroup) octaveContainer;
                int[] resIds = new int[]{R.id.piano_c_sharp, R.id.piano_d_sharp, R.id.piano_f_sharp,
                        R.id.piano_g_sharp, R.id.piano_a_sharp, R.id.piano_c, R.id.piano_d,
                        R.id.piano_e, R.id.piano_f, R.id.piano_g, R.id.piano_a, R.id.piano_b};
                int[] offsets = new int[]{2, 4, 7, 9, 11, 1, 3, 5, 6, 8, 10, 12};
                for(int i = 0; i < 12; i++) {
                    View keyView = vg.findViewById(resIds[i]);
                    Rect r = new Rect();
                    keyView.getLocalVisibleRect(r);
                    vg.offsetDescendantRectToMyCoords(keyView, r);
                    rv.offsetDescendantRectToMyCoords(octaveContainer, r);

                    if(r.contains((int) e.getX(), (int) e.getY())) {
                        NoteEvent noteEvent = new NoteEvent(octave * 12 + offsets[i], NoteEvent.ACTION_BEGIN);
                        clickPostHook.accept(noteEvent);
                        break;
                    }
                }
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
        });
        pianoAdapter = new PianoAdapter(clickPostHook);
        pianoContainer.setAdapter(pianoAdapter);
        return rootView;
    }
}
