package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulTouchListener;
import libre.sampler.models.NoteEvent;

public class PianoAdapter extends RecyclerView.Adapter<PianoAdapter.ViewHolder> {
    private static final int NUM_OCTAVES = 8;
    private Consumer<NoteEvent> clickPostHook;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rootView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            rootView = v;
        }
    }

    public PianoAdapter(Consumer<NoteEvent> clickPostHook) {
        this.clickPostHook = clickPostHook;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_piano_octave, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ((TextView) holder.rootView.getChildAt(0)).setText("C" + position);
        // register black keys listeners last
        int[] resIds = new int[]{R.id.piano_c, R.id.piano_d, R.id.piano_e, R.id.piano_f,
                R.id.piano_g, R.id.piano_a, R.id.piano_b, R.id.piano_c_sharp, R.id.piano_d_sharp,
                R.id.piano_f_sharp, R.id.piano_g_sharp, R.id.piano_a_sharp};
        int[] offsets = new int[]{1, 3, 5, 6, 8, 10, 12, 2, 4, 7, 9, 11};
        for(int i = 0; i < 12; i++) {
            int keyNum = position * 12 + offsets[i];
            View v = holder.rootView.findViewById(resIds[i]);
            v.setOnTouchListener(
                    new StatefulTouchListener<Integer>(keyNum) {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            NoteEvent noteEvent;
                            switch(event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                case MotionEvent.ACTION_POINTER_DOWN:
                                    noteEvent = new NoteEvent(this.data, NoteEvent.ACTION_BEGIN);
                                    clickPostHook.accept(noteEvent);
                                    break;
                                case MotionEvent.ACTION_MOVE:
                                    // todo handle slide
                                    break;
                                case MotionEvent.ACTION_CANCEL:
                                case MotionEvent.ACTION_UP:
                                case MotionEvent.ACTION_POINTER_UP:
                                    noteEvent = new NoteEvent(this.data, NoteEvent.ACTION_END);
                                    clickPostHook.accept(noteEvent);
                                    break;
                            }
                            return false;
                        }
                    }
            );
        }
    }

    @Override
    public int getItemCount() {
        return NUM_OCTAVES;
    }
}
