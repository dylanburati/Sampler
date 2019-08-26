package libre.sampler.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.transition.ChangeBounds;
import androidx.transition.TransitionManager;
import libre.sampler.R;

import static libre.sampler.utils.ViewUtil.dpToPxSize;

public class IconNavigationPanel extends RelativeLayout {
    private static final int INDICATOR_WIDTH_DP = 2;
    private static final int BORDER_WIDTH_DP = 2;

    private final List<View> clickableItems = new ArrayList<>();
    private final View navIndicator;
    private final View navBorder;
    private final View.OnClickListener childClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setSelectedIcon(v);
        }
    };
    private OnItemSelectedListener externalListener;

    private int indicatorWidth;
    private int borderWidth;

    public IconNavigationPanel(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        setVerticalScrollBarEnabled(false);

        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        float density = displayMetrics.density;
        indicatorWidth = dpToPxSize(density, INDICATOR_WIDTH_DP);
        borderWidth = dpToPxSize(density, BORDER_WIDTH_DP);

        navBorder = new View(context);
        LayoutParams navBorderParams = new LayoutParams(borderWidth, LayoutParams.MATCH_PARENT);
        navBorderParams.addRule(ALIGN_PARENT_RIGHT);

        navIndicator = new View(context);
        LayoutParams navIndicatorParams = new LayoutParams(indicatorWidth, 0);
        navIndicatorParams.addRule(ALIGN_PARENT_RIGHT);

        addView(navBorder, 0, navBorderParams);
        addView(navIndicator, 1, navIndicatorParams);

        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconNavigationPanel, 0, 0);
            navIndicator.setBackgroundColor(a.getColor(R.styleable.IconNavigationPanel_indicator_color, Color.WHITE));
            navBorder.setBackgroundColor(a.getColor(R.styleable.IconNavigationPanel_border_color, res.getColor(R.color.colorBorderOnBackground)));
            a.recycle();
        }
    }

    @Override
    public void onViewAdded(View child) {
        if(child != navBorder && child != navIndicator) {
            clickableItems.add(child);
            child.setOnClickListener(childClickListener);
            setRelativeLayoutRuleForChild(clickableItems.size() - 1);
        }
    }

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        int removeIdx = clickableItems.indexOf(child);
        if(removeIdx != -1) {
            setRelativeLayoutRuleForChild(removeIdx);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if(selectedIconView == null && clickableItems.size() > 0) {
            setSelectedIcon(clickableItems.get(0));
        }
    }

    private void setRelativeLayoutRuleForChild(int i) {
        View child = clickableItems.get(i);
        RelativeLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if(i == 0) {
            lp.removeRule(BELOW);
            lp.addRule(ALIGN_PARENT_TOP);
        } else {
            lp.removeRule(BELOW);
            lp.addRule(BELOW, clickableItems.get(i - 1).getId());
        }
        child.setLayoutParams(lp);
    }

    private View selectedIconView;
    public void setSelectedIcon(View view) {
        if(view == null) {
            return;
        }

        if(view != selectedIconView) {
            if(selectedIconView != null) {
                selectedIconView.setSelected(false);
            }
            selectedIconView = view;
            view.setSelected(true);
        }

        double underflow = 0.5 * (view.getPaddingTop() + view.getPaddingBottom());
        int h = (int) (view.getHeight() - underflow);
        int t = (int) (view.getTop() + 0.5 * underflow);
        LayoutParams layoutParams = (LayoutParams) navIndicator.getLayoutParams();
        layoutParams.height = h;
        layoutParams.topMargin = t;
        navIndicator.setLayoutParams(layoutParams);
        TransitionManager.beginDelayedTransition(this, new ChangeBounds().setDuration(150));

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
