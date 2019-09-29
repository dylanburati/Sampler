package libre.sampler.views;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import libre.sampler.R;

public class IconNavigationPanel {
    private final List<View> clickableItems = new ArrayList<>();
    private final View navIndicator;
    private final ConstraintLayout iconContainer;
    private final View.OnClickListener childClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setSelectedIcon(v);
        }
    };
    private OnItemSelectedListener externalListener;

    public IconNavigationPanel(ConstraintLayout iconContainer) {
        this.iconContainer = iconContainer;
        this.navIndicator = iconContainer.findViewById(R.id.nav_indicator);
        View navBorder = iconContainer.findViewById(R.id.nav_border);
        int size = iconContainer.getChildCount();
        for(int i = 0; i < size; i++) {
            View v = iconContainer.getChildAt(i);
            if(v != navBorder && v != navIndicator) {
                clickableItems.add(v);
                v.setOnClickListener(childClickListener);
            }
        }

        iconContainer.post(new Runnable() {
            @Override
            public void run() {
                if(!clickableItems.isEmpty()) {
                    setSelectedIcon(clickableItems.get(0));
                }
            }
        });
    }

    private View selectedIconView;
    public void setSelectedIcon(View view) {
        if(view == null || view == selectedIconView) {
            return;
        }

        if(selectedIconView != null) {
            selectedIconView.setSelected(false);
        }
        selectedIconView = view;
        view.setSelected(true);

        double underflow = 0.5 * (view.getPaddingTop() + view.getPaddingBottom());
        int h = (int) (view.getHeight() - underflow);
        int t = (int) (view.getTop() + 0.5 * underflow);
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) navIndicator.getLayoutParams();
        layoutParams.height = h;
        layoutParams.topMargin = t;
        navIndicator.setLayoutParams(layoutParams);
        TransitionManager.beginDelayedTransition(this.iconContainer, new ChangeBounds().setDuration(150));

        if(externalListener != null) {
            externalListener.onSelect(view);
        }
    }

    public interface OnItemSelectedListener {
        void onSelect(View v);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener fn) {
        this.externalListener = fn;
    }
}
