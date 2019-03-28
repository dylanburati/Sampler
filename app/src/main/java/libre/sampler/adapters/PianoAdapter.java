package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.models.NoteEvent;

public class PianoAdapter extends RecyclerView.Adapter<PianoAdapter.ViewHolder> {
    private static final int NUM_OCTAVES = 8;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rootView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            rootView = v;
        }
    }

    public PianoAdapter() {
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
    }

    @Override
    public int getItemCount() {
        return NUM_OCTAVES;
    }
}
