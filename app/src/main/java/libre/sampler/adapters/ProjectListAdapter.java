package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.models.Project;
import libre.sampler.utils.AdapterLoader;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ViewHolder> implements AdapterLoader.Loadable<Project> {
    public List<Project> items;
    private Consumer<Project> clickPostHook;

    @Override
    public List<Project> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private TextView nameTextView;
        private TextView mtimeTextView;
        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            nameTextView = (TextView) v.findViewById(R.id.text);
            mtimeTextView = (TextView) v.findViewById(R.id.mtime);
        }
    }

    public ProjectListAdapter(List<Project> items, Consumer<Project> clickPostHook) {
        this.items = items;
        this.clickPostHook = clickPostHook;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_project_list_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Project item = items.get(position);
        holder.rootView.setOnClickListener(new StatefulClickListener<Project>(item) {
            @Override
            public void onClick(View v) {
                clickPostHook.accept(this.data);
            }
        });
        holder.nameTextView.setText(item.name);
        holder.mtimeTextView.setText(item.getRelativeTime());
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }


}
