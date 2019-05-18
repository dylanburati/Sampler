package libre.sampler.utils;

import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class AdapterLoader {
    public interface Loadable<T> {
        public List<T> items();
    }

    public static <T> void insertItem(Loadable<T> adapter, T e) {
        int insertIdx = adapter.items().size();
        AdapterLoader.insertItem(adapter, insertIdx, e);
    }

    public static <T> void insertItem(Loadable<T> adapter, int insertIdx, T e) {
        adapter.items().add(insertIdx, e);
        ((RecyclerView.Adapter) adapter).notifyItemInserted(insertIdx);
    }

    public static <T> void insertAll(Loadable<T> adapter, List<T> e) {
        int insertIdx = adapter.items().size();
        AdapterLoader.insertAll(adapter, insertIdx, e);
    }

    public static <T> void insertAll(Loadable<T> adapter, int insertIdx, List<T> e) {
        adapter.items().addAll(insertIdx, e);
        ((RecyclerView.Adapter) adapter).notifyItemRangeInserted(insertIdx, e.size());
    }

    public static <T> void removeItem(Loadable<T> adapter, int removeIdx) {
        adapter.items().remove(removeIdx);
        ((RecyclerView.Adapter) adapter).notifyItemRemoved(removeIdx);
    }

    public static <T> void removeItem(Loadable<T> adapter, T e) {
        int removeIdx = adapter.items().indexOf(e);
        if(removeIdx != -1) {
            adapter.items().remove(removeIdx);
            ((RecyclerView.Adapter) adapter).notifyItemRemoved(removeIdx);
        }
    }

    public static <T> void clear(Loadable<T> adapter) {
        int removeLen = adapter.items().size();
        adapter.items().clear();
        ((RecyclerView.Adapter) adapter).notifyItemRangeRemoved(0, removeLen);
    }
}
