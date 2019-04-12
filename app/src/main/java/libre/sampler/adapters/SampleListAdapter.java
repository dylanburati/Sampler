package libre.sampler.adapters;

import android.animation.LayoutTransition;
import android.content.res.Resources;
import android.transition.AutoTransition;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.transition.TransitionPropagation;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.models.Sample;
import libre.sampler.utils.AdapterLoader;

public class SampleListAdapter extends RecyclerView.Adapter<SampleListAdapter.ViewHolder> implements AdapterLoader.Loadable<Sample> {
    public List<Sample> items;
    private final Set<ViewHolder> viewHolderSet;

    @Override
    public List<Sample> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public boolean isCollapsed = true;
        private LinearLayout rootView;
        private View collapsedView;
        private View expandIcon;
        private View expandedView;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            collapsedView = v.findViewById(R.id.collapsed_sample_data);
            expandIcon = v.findViewById(R.id.icon_expand);
            expandedView = v.findViewById(R.id.expanded_sample_data);
        }

        public void collapse() {
            if(!isCollapsed) {
                Transition tr = new ChangeBounds();
                tr.setDuration(300);
                TransitionManager.beginDelayedTransition(rootView, tr);
                expandedView.setVisibility(View.GONE);
                expandIcon.setBackground(rootView.getContext().getDrawable(R.drawable.ic_keyboard_arrow_down));
                isCollapsed = true;
            }
        }

        public void expand() {
            if(isCollapsed) {
                Transition tr = new ChangeBounds();
                tr.setDuration(300);
                TransitionManager.beginDelayedTransition(rootView, tr);
                expandedView.setVisibility(View.VISIBLE);
                expandIcon.setBackground(rootView.getContext().getDrawable(R.drawable.ic_keyboard_arrow_up));
                isCollapsed = false;
            }
        }
    }

    public SampleListAdapter(List<Sample> items) {
        this.items = items;
        this.viewHolderSet = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_sample_list_tile, parent, false);
        ViewHolder vh = new ViewHolder(v);
        viewHolderSet.add(vh);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.collapsedView.setOnClickListener(
                new StatefulClickListener<ViewHolder>(holder) {
                    @Override
                    public void onClick(View v) {
                        if(this.data.isCollapsed) {
                            this.data.expand();
                            for(ViewHolder other : viewHolderSet) {
                                if(!this.data.equals(other)) {
                                    other.collapse();
                                }
                            }
                        } else {
                            this.data.collapse();
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}
