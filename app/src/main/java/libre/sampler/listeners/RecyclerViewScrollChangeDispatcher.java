package libre.sampler.listeners;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;

import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewScrollChangeDispatcher {
    private final WeakReference<RecyclerView> viewRef;
    private ViewTreeObserver viewTreeObserver;

    private Rect tmpRect = new Rect();
    private Point tmpOldLocation = new Point();
    private Point tmpLocation = new Point();

    private ViewTreeObserver.OnScrollChangedListener internalListener;
    private ScrollChangeListener listener;

    public RecyclerViewScrollChangeDispatcher(RecyclerView recyclerView) {
        this.viewRef = new WeakReference<>(recyclerView);

        internalListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                RecyclerView view = viewRef.get();
                if(view == null) {
                    onDestroyView();
                    return;
                }
                RecyclerView.LayoutManager layoutManager = view.getLayoutManager();
                if(layoutManager != null && layoutManager.getChildCount() > 0) {
                    View child = layoutManager.getChildAt(0);
                    tmpLocation.x = -layoutManager.getDecoratedLeft(child);

                    view.getLocalVisibleRect(tmpRect);
                    tmpLocation.y = tmpRect.top;

                    if(!tmpLocation.equals(tmpOldLocation) && listener != null) {
                        listener.onScrollChange(tmpLocation.x, tmpLocation.y, tmpOldLocation.x, tmpOldLocation.y);
                    }

                    tmpOldLocation.set(tmpLocation.x, tmpLocation.y);
                }
            }
        };

        updateViewTreeObserver();
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(viewTreeObserver == null || !viewTreeObserver.isAlive()) {
                    updateViewTreeObserver();
                }
            }
        });
    }

    private void updateViewTreeObserver() {
        viewTreeObserver = viewRef.get().getViewTreeObserver();
        viewTreeObserver.addOnScrollChangedListener(internalListener);
    }

    public void onDestroyView() {
        if(viewTreeObserver != null && viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnScrollChangedListener(internalListener);
        }
        internalListener = null;
        viewTreeObserver = null;
    }

    public void setListener(ScrollChangeListener listener) {
        this.listener = listener;
    }
}
