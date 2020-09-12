package libre.sampler.adapters;

import android.animation.ObjectAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;

public class AddInstrumentListAdapter extends RecyclerView.Adapter<AddInstrumentListAdapter.ViewHolder> {
    private List<Project> projects;
    private final Map<String, List<Instrument>> instrumentMap = new HashMap<>();

    private final Set<Instrument> selectedInstruments = new HashSet<>();

    private final List<Instrument> expandedInstruments = new ArrayList<>();
    private int expandedStartIndex = -1;
    private int expandedEndIndex = -1;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private TextView nameTextView;
        private CheckBox checkBox;
        public int viewType;

        private List<Instrument> instruments;

        public ViewHolder(LinearLayout v, int viewType) {
            super(v);
            rootView = v;
            this.viewType = viewType;
            nameTextView = v.findViewById(R.id.text);
            checkBox = v.findViewById(R.id.checkbox);
        }

        private void setInstruments(List<Instrument> instruments) {
            this.instruments = instruments;
        }

        private List<Instrument> getInstruments() {
            return this.instruments;
        }
    }

    public AddInstrumentListAdapter() {
        this.projects = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        if(position >= expandedStartIndex && position < expandedEndIndex) {
            return 1;
        }
        return 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v;
        if(viewType == 0) {
            v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.component_add_instrument_expand_tile, parent, false);
        } else {
            v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.component_add_instrument_list_tile, parent, false);
        }
        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        if(holder.viewType == 0) {
            Project project = getProject(position);
            holder.setInstruments(instrumentMap.get(project.id));
            holder.nameTextView.setText(project.name);
            holder.rootView.setOnClickListener(new StatefulClickListener<ViewHolder>(holder) {
                @Override
                public void onClick(View v) {
                    toggleExpanded(this.data);
                }
            });
        } else {
            Instrument instrument = getInstrument(position);
            holder.setInstruments(Collections.singletonList(instrument));
            holder.nameTextView.setText(instrument.name);
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedInstruments.containsAll(holder.getInstruments()));
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    selectedInstruments.addAll(holder.getInstruments());
                } else {
                    selectedInstruments.removeAll(holder.getInstruments());
                }
                updateCheckBoxes(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.projects.size() + expandedEndIndex - expandedStartIndex;
    }

    private RecyclerView recyclerView;
    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        this.recyclerView = null;
    }

    private Project getProject(int position) {
        int i = position;
        if(i >= expandedEndIndex) {
            i -= expandedEndIndex - expandedStartIndex;
        } else if(i >= expandedStartIndex) {
            throw new AssertionError("Top-level project not found");
        }
        return projects.get(i);
    }

    private Instrument getInstrument(int position) {
        if(position < expandedStartIndex || position >= expandedEndIndex) {
            throw new AssertionError("Nested instrument not found");
        }
        return expandedInstruments.get(position - expandedStartIndex);
    }

    private void updateCheckBoxes(int ignorePosition) {
        for(int pos = 0; pos < getItemCount(); pos++) {
            if(pos == ignorePosition) {
                continue;
            }
            ViewHolder vh = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(pos);
            if(vh == null) {
                continue;
            }
            vh.checkBox.setChecked(selectedInstruments.containsAll(vh.getInstruments()));
        }
    }

    public void setProjectsAndInstruments(List<Project> projects, List<Instrument> instruments) {
        for(Instrument t : instruments) {
            List<Instrument> list = instrumentMap.get(t.projectId);
            if(list == null) {
                list = new ArrayList<>();
                instrumentMap.put(t.projectId, list);
            }
            list.add(t);
        }
        this.projects = new ArrayList<>();
        for(Project p : projects) {
            if(instrumentMap.containsKey(p.id)) {
                this.projects.add(p);
            }
        }
        expandedStartIndex = -1;
        expandedEndIndex = -1;
        expandedInstruments.clear();
        selectedInstruments.clear();
        notifyDataSetChanged();
    }

    private void toggleExpanded(ViewHolder vh) {
        int position = vh.getAdapterPosition();
        // always collapse current
        if(expandedEndIndex > expandedStartIndex) {
            expandedInstruments.clear();
            notifyItemRangeRemoved(expandedStartIndex, expandedEndIndex - expandedStartIndex);
        }
        // expand new if different
        boolean willExpand = (position != expandedStartIndex - 1);
        Project project = getProject(position);
        if(willExpand) {
            int newPosition = position;
            if(newPosition >= expandedEndIndex) {
                newPosition -= expandedEndIndex - expandedStartIndex;
            }
            List<Instrument> list = instrumentMap.get(project.id);
            if(list != null) {
                expandedInstruments.addAll(list);
            }
            expandedStartIndex = newPosition + 1;
            expandedEndIndex = expandedStartIndex + expandedInstruments.size();
            if(!expandedInstruments.isEmpty()) {
                notifyItemRangeInserted(expandedStartIndex, expandedInstruments.size());
            }
        } else {
            expandedStartIndex = -1;
            expandedEndIndex = -1;
        }

        for(int i = 0; i < getItemCount(); i++) {
            ViewHolder ovh = (ViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if(ovh == null) {
                continue;
            }
            View icon = ovh.rootView.findViewById(R.id.icon_dropdown);
            if(icon == null) {
                continue;
            }
            if(ovh == vh && willExpand) {
                ObjectAnimator.ofFloat(icon, "rotation", 180)
                        .setDuration(120).start();
            } else {
                ObjectAnimator.ofFloat(icon, "rotation", 0)
                        .setDuration(120).start();
            }
        }
        Log.d("ExpandIndices", String.format("[%d, %d)", expandedStartIndex, expandedEndIndex));
    }

    public Set<Instrument> getSelectedInstruments() {
        return selectedInstruments;
    }
}
