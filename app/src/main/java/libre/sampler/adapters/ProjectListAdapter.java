package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.ProjectClickListener;
import libre.sampler.models.Project;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ViewHolder> {
    public List<Project> items;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private TextView nameTextView;
        private TextView mtimeTextView;
        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            nameTextView = (TextView) v.getChildAt(0);
            mtimeTextView = (TextView) v.getChildAt(1);
        }
    }

    public ProjectListAdapter(List<Project> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_list_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project item = items.get(position);
        holder.rootView.setOnClickListener(new ProjectClickListener(item));
        holder.nameTextView.setText(item.name);
        holder.mtimeTextView.setText(item.getRelativeTime());
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public void insertItem(Project e) {
        int insertIdx = this.items.size();
        this.items.add(e);
        this.notifyItemInserted(insertIdx);
    }

    public void insertItem(int insertIdx, Project e) {
        this.items.add(insertIdx, e);
        this.notifyItemInserted(insertIdx);
    }

    public void insertAll(List<Project> e) {
        int insertIdx = this.items.size();
        this.items.addAll(e);
        this.notifyItemRangeInserted(insertIdx, e.size());
    }

    public void insertAll(int insertIdx, List<Project> e) {
        this.items.addAll(insertIdx, e);
        this.notifyItemRangeInserted(insertIdx, e.size());
    }

    public void removeItem(int removeIdx) {
        this.items.remove(removeIdx);
        this.notifyItemRemoved(removeIdx);
    }
}
