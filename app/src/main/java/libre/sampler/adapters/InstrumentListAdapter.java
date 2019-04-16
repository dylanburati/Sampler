package libre.sampler.adapters;

import android.content.res.Resources;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.listeners.StatefulClickListener;
import libre.sampler.listeners.StatefulLongClickListener;
import libre.sampler.models.Instrument;
import libre.sampler.utils.AdapterLoader;

public class InstrumentListAdapter extends RecyclerView.Adapter<InstrumentListAdapter.ViewHolder> implements AdapterLoader.Loadable<Instrument> {
    public List<Instrument> items;
    private final Set<ViewHolder> viewHolderSet;

    private int activateOnBind = -1;

    private Consumer<Instrument> editPostHook;
    private Consumer<Instrument> selectPostHook;
    private Runnable createPostHook;
    public boolean autoScrollOnInsert = false;  // todo

    @Override
    public List<Instrument> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private View iconAddView;
        private TextView nameTextView;

        public ViewHolder(LinearLayout v) {
            super(v);
            rootView = v;
            iconAddView = v.findViewById(R.id.icon_add);
            nameTextView = (TextView) v.findViewById(R.id.text);
        }
    }

    public InstrumentListAdapter(List<Instrument> items, Consumer<Instrument> editPostHook,
                                 Consumer<Instrument> selectPostHook, Runnable createPostHook) {
        items.add(0, null);
        this.items = items;
        this.editPostHook = editPostHook;
        this.selectPostHook = selectPostHook;
        this.createPostHook = createPostHook;

        this.viewHolderSet = new HashSet<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.component_instrument_list_tile, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        viewHolderSet.add(holder);
        Resources res = holder.rootView.getResources();
        if(position == 0) {
            holder.iconAddView.setVisibility(View.VISIBLE);
            holder.rootView.setPadding((int) res.getDimension(R.dimen.margin3), holder.rootView.getPaddingTop(),
                    (int) res.getDimension(R.dimen.margin3), holder.rootView.getPaddingBottom());
            holder.nameTextView.setText(R.string.instrument_create);
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createPostHook.run();
                }
            });
        } else {
            if(position == activateOnBind) {
                holder.nameTextView.setActivated(true);
            }
            Instrument item = items.get(position);
            holder.iconAddView.setVisibility(View.GONE);
            holder.rootView.setPadding((int) res.getDimension(R.dimen.margin4), holder.rootView.getPaddingTop(),
                    (int) res.getDimension(R.dimen.margin4), holder.rootView.getPaddingBottom());
            holder.rootView.setOnClickListener(new StatefulClickListener<Instrument>(item) {
                @Override
                public void onClick(View v) {
                    selectPostHook.accept(this.data);
                }
            });
            holder.rootView.setOnLongClickListener(new StatefulLongClickListener<Instrument>(item) {
                @Override
                public boolean onLongClick(View v) {
                    editPostHook.accept(this.data);
                    return true;
                }
            });
            holder.nameTextView.setText(item.name);
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public void activateItem(int position) {
        boolean done = false;
        for(ViewHolder vh : viewHolderSet) {
            if(vh != null) {
                if(vh.getAdapterPosition() == position) {
                    vh.nameTextView.setActivated(true);
                    done = true;
                } else {
                    vh.nameTextView.setActivated(false);
                }
            }
        }
        if(!done) {
            activateOnBind = position;
        } else {
            activateOnBind = -1;
        }
    }
}
