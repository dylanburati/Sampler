package libre.sampler.adapters;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.views.PianoRecyclerView;

public class PianoAdapter extends RecyclerView.Adapter<PianoAdapter.ViewHolder> implements PianoRecyclerView.OnZoomListener {
    private static final int NUM_OCTAVES = 8;
    private final float baseKeyWidthBlack;
    private final float baseKeyWidthWhite;
    private final float[] baseKeyMargins;
    private float currentScaleFactor = 1.0f;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rootView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            rootView = v;
        }
    }

    public PianoAdapter(Resources res) {
        baseKeyWidthBlack = res.getDimension(R.dimen.piano_keywidth_b);
        baseKeyWidthWhite = res.getDimension(R.dimen.piano_keywidth_w1);
        baseKeyMargins = new float[]{
                res.getDimension(R.dimen.piano_keymargin_67),
                res.getDimension(R.dimen.piano_keymargin_33),
                res.getDimension(R.dimen.piano_keymargin_75),
                res.getDimension(R.dimen.piano_keymargin_50),
                res.getDimension(R.dimen.piano_keymargin_25)
        };
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
        ((TextView) holder.rootView.findViewById(R.id.piano_octave_label)).setText("C" + (position + 1));
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        setOctaveScale(holder.rootView, currentScaleFactor);
    }

    @Override
    public int getItemCount() {
        return NUM_OCTAVES;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if(recyclerView instanceof PianoRecyclerView) {
            ((PianoRecyclerView) recyclerView).setZoomListener(this);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if(recyclerView instanceof PianoRecyclerView) {
            ((PianoRecyclerView) recyclerView).setZoomListener(null);
        }
    }

    private void setOctaveScale(RelativeLayout octave, float scaleFactor) {
        for(int j = 0; j < KEY_IDS.length; j++) {
            View key = octave.findViewById(KEY_IDS[j]);
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) key.getLayoutParams();
            if(lp.width > 0) {
                // not WRAP_CONTENT or MATCH_PARENT
                lp.width = Math.round(scaleFactor * (j < 5 ? baseKeyWidthBlack : baseKeyWidthWhite));
                lp.leftMargin = getKeyLeftMargin(j, lp.width);
                key.setLayoutParams(lp);
            }
        }
    }

    private static final int[] KEY_IDS = new int[]{R.id.piano_c_sharp, R.id.piano_d_sharp, R.id.piano_f_sharp,
            R.id.piano_g_sharp, R.id.piano_a_sharp, R.id.piano_c, R.id.piano_d,
            R.id.piano_e, R.id.piano_f, R.id.piano_g, R.id.piano_a, R.id.piano_b};

    private int getKeyLeftMargin(int index, int keyWidth) {
        if(index >= 5) {
            return 0;
        }
        return Math.round(((float) keyWidth / baseKeyWidthBlack) * baseKeyMargins[index]);
    }

    private float translateX = 0.0f;
    @Override
    public boolean onZoom(RecyclerView rv, float scaleFactor, float focusX) {
        translateX -= Math.round(((scaleFactor / this.currentScaleFactor) - 1) * focusX);
        this.currentScaleFactor = scaleFactor;
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        if(lm == null) {
            return false;
        }

        int n = lm.getChildCount();
        for(int i = 0; i < n; i++) {
            View v = lm.getChildAt(i);
            if(!(v instanceof RelativeLayout)) {
                continue;
            }
            RelativeLayout octave = (RelativeLayout) v;

            setOctaveScale(octave, scaleFactor);
        }
        int offset = Math.round(translateX);
        lm.offsetChildrenHorizontal(offset);
        translateX -= offset;
        return true;
    }
}
