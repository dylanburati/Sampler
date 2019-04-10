package libre.sampler.adapters;

import android.content.res.Resources;
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
import libre.sampler.models.Instrument;
import libre.sampler.models.Project;
import libre.sampler.utils.AdapterLoader;

public class InstrumentListAdapter extends RecyclerView.Adapter<InstrumentListAdapter.ViewHolder> implements AdapterLoader.Loadable<Instrument> {
    public List<Instrument> items;
    private Consumer<Instrument> clickPostHook;
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
            iconAddView = v.getChildAt(0);
            nameTextView = (TextView) v.getChildAt(1);
        }
    }

    public InstrumentListAdapter(List<Instrument> items, Consumer<Instrument> clickPostHook) {
        items.add(0, null);
        this.items = items;
        this.clickPostHook = clickPostHook;
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
        if(position == 0) {
            Resources res = holder.rootView.getResources();
            holder.iconAddView.setVisibility(View.VISIBLE);
            holder.rootView.setPadding((int) res.getDimension(R.dimen.margin3), holder.rootView.getPaddingTop(),
                    (int) res.getDimension(R.dimen.margin3), holder.rootView.getPaddingBottom());
            holder.nameTextView.setText(R.string.instrument_create);
        } else {
            Instrument item = items.get(position);
            holder.rootView.setOnClickListener(new StatefulClickListener<Instrument>(item) {
                @Override
                public void onClick(View v) {
                    clickPostHook.accept(this.data);
                }
            });
            holder.nameTextView.setText(item.name);
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }
}
