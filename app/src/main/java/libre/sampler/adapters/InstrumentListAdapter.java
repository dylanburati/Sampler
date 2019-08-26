package libre.sampler.adapters;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.models.Instrument;
import libre.sampler.utils.AdapterLoader;

public class InstrumentListAdapter extends RecyclerView.Adapter<InstrumentListAdapter.ViewHolder> implements AdapterLoader.Loadable<Instrument> {
    public List<Instrument> items;
    private final Set<ViewHolder> viewHolderSet;

    private final InstrumentActionConsumer instrumentActionConsumer;

    private Instrument activateOnBind;

    @Override
    public List<Instrument> items() {
        return items;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private LinearLayout rootView;
        private TextView nameTextView;
        public int viewType;

        private Instrument instrument;
        private PopupMenu popupMenu;

        public ViewHolder(LinearLayout v, int viewType) {
            super(v);
            rootView = v;
            this.viewType = viewType;
            nameTextView = (TextView) v.findViewById(R.id.text);
        }

        public void setPopupMenu(PopupMenu menu) {
            this.popupMenu = menu;
        }

        public PopupMenu getPopupMenu() {
            return this.popupMenu;
        }
    }

    public InstrumentListAdapter(List<Instrument> items, InstrumentActionConsumer instrumentActionConsumer) {
        items.add(0, null);
        this.items = items;
        this.instrumentActionConsumer = instrumentActionConsumer;

        this.viewHolderSet = new HashSet<>();
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) {
            return 0;
        }
        return 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout v;
        if(viewType == 0) {
            v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.component_new_instrument_list_tile, parent, false);
        } else {
            v = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.component_instrument_list_tile, parent, false);
        }
        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        viewHolderSet.add(holder);
        if(holder.viewType == 0) {
            holder.rootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instrumentActionConsumer.startCreate();
                }
            });
        } else {
            holder.instrument = items.get(position);
            holder.nameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instrumentActionConsumer.select(holder.instrument);
                }
            });
            PopupMenu menu = new PopupMenu(holder.rootView.getContext(), holder.rootView.findViewById(R.id.more_vert_menu));
            menu.inflate(R.menu.menu_instrument);
            menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if(item.getItemId() == R.id.instrument_edit) {
                        instrumentActionConsumer.startRename(holder.instrument);
                    } else if(item.getItemId() == R.id.instrument_export) {
                        instrumentActionConsumer.startExport(holder.instrument);
                    }
                    return true;
                }
            });
            holder.setPopupMenu(menu);
            holder.rootView.findViewById(R.id.more_vert_menu).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.getPopupMenu().show();
                }
            });
            // holder.rootView.setOnLongClickListener(new StatefulLongClickListener<Instrument>(holder.instrument) {
            //     @Override
            //     public boolean onLongClick(View v) {
            //         editPostHook.accept(this.data);
            //         return true;
            //     }
            // });
            holder.nameTextView.setText(holder.instrument.name);
            if(holder.instrument == this.activateOnBind) {
                activateItem(holder);
            }
        }
    }

    @Override
    public int getItemCount() {
        return this.items.size();
    }

    public void activateInstrument(Instrument instrument) {
        this.activateOnBind = instrument;
        if(instrument == null) {
            return;
        }
        for(ViewHolder vh : viewHolderSet) {
            if(vh != null) {
                if(instrument == vh.instrument) {
                    activateItem(vh);
                } else {
                    vh.nameTextView.setActivated(false);
                }
            }
        }
    }

    private void activateItem(ViewHolder vh) {
        vh.nameTextView.setActivated(true);
        this.activateOnBind = null;
    }

    public interface InstrumentActionConsumer {
        void startCreate();
        void startRename(Instrument instrument);
        void startExport(Instrument instrument);
        void select(Instrument instrument);
    }
}
