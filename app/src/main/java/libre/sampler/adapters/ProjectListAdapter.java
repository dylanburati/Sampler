package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.models.Project;
import libre.sampler.utils.AdapterLoader;

public class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.ViewHolder> implements AdapterLoader.Loadable<Project> {
    public List<Project> items;
    private ProjectActionConsumer projectActionConsumer;

    @Override
    public List<Project> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private TextView nameTextView;
        private TextView mtimeTextView;
        private PopupMenu popupMenu;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            nameTextView = (TextView) v.findViewById(R.id.text);
            mtimeTextView = (TextView) v.findViewById(R.id.mtime);
        }

        public PopupMenu getPopupMenu() {
            return popupMenu;
        }

        public void setPopupMenu(PopupMenu popupMenu) {
            this.popupMenu = popupMenu;
        }
    }

    public ProjectListAdapter(List<Project> items, ProjectActionConsumer projectActionConsumer) {
        this.items = items;
        this.projectActionConsumer = projectActionConsumer;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_project_list_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        Project item = items.get(position);
        holder.rootView.setOnClickListener(new StatefulClickListener<Project>(item) {
            @Override
            public void onClick(View v) {
                projectActionConsumer.open(this.data);
            }
        });
        holder.nameTextView.setText(item.name);
        holder.mtimeTextView.setText(item.getRelativeTime());
        PopupMenu menu = new PopupMenu(holder.rootView.getContext(), holder.rootView.findViewById(R.id.mtime));
        menu.inflate(R.menu.menu_project_tile);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Project project = items.get(holder.getAdapterPosition());
                if(item.getItemId() == R.id.project_edit) {
                    projectActionConsumer.startRename(project);
                } else if(item.getItemId() == R.id.project_delete) {
                    projectActionConsumer.delete(project);
                }
                return true;
            }
        });
        holder.setPopupMenu(menu);
        holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                holder.getPopupMenu().show();
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public interface ProjectActionConsumer {
        void startRename(Project project);
        void open(Project project);
        void delete(Project project);
    }
}
