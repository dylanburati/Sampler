package libre.sampler.listeners;

import android.graphics.Point;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewScrollChangeDispatcher {
    private RecyclerView view;
    private ViewTreeObserver viewTreeObserver;

    private Point tmpOldLocation = new Point();
    private Point tmpLocation = new Point();

    private ViewTreeObserver.OnScrollChangedListener internalListener;
    private ScrollChangeListener listener;

    public RecyclerViewScrollChangeDispatcher(final RecyclerView view) {
        this.view = view;

        internalListener = new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                RecyclerView.LayoutManager layoutManager = view.getLayoutManager();
                if(layoutManager != null && layoutManager.getChildCount() > 0) {
                    View child = layoutManager.getChildAt(0);
                    tmpLocation.x = -layoutManager.getDecoratedLeft(child);
                    tmpLocation.y = -layoutManager.getDecoratedTop(child);

                    if(!tmpLocation.equals(tmpOldLocation)) {
                        listener.onScrollChange(tmpLocation.x, tmpLocation.y, tmpOldLocation.x, tmpOldLocation.y);
                    }

                    tmpOldLocation.set(tmpLocation.x, tmpLocation.y);
                }
            }
        };

        updateViewTreeObserver();
        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(viewTreeObserver == null || !viewTreeObserver.isAlive()) {
                    updateViewTreeObserver();
                }
            }
        });
    }

    private void updateViewTreeObserver() {
        viewTreeObserver = view.getViewTreeObserver();
        viewTreeObserver.addOnScrollChangedListener(internalListener);
    }

    public void setListener(ScrollChangeListener listener) {
        this.listener = listener;
    }
}
