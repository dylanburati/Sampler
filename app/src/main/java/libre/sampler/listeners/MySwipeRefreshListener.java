package libre.sampler.listeners;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MySwipeRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
    private RefreshPostHook refreshPostHook;

    public MySwipeRefreshListener(RefreshPostHook refreshPostHook) {
        this.refreshPostHook = refreshPostHook;
    }

    @Override
    public void onRefresh() {
        // doRefresh();
        refreshPostHook.afterRefresh();
    }
}
