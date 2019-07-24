package libre.sampler.views;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import libre.sampler.R;

public class IconNavigationPanel extends RelativeLayout {
    private static final int INDICATOR_WIDTH_DP = 2;
    private static final int BORDER_WIDTH_DP = 2;

    private final List<View> clickableItems = new ArrayList<>();
    private final NavIndicator navIndicator;
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

        navIndicator = new NavIndicator(context);
        RelativeLayout.LayoutParams navIndicatorParams = new LayoutParams(indicatorWidth, LayoutParams.MATCH_PARENT);
        navIndicatorParams.addRule(ALIGN_PARENT_END);
        super.addView(navIndicator, 0, navIndicatorParams);

        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.IconNavigationPanel, 0, 0);
            navIndicator.setIndicatorColor(a.getColor(R.styleable.IconNavigationPanel_indicator_color, Color.WHITE));
            navIndicator.setBorderColor(a.getColor(R.styleable.IconNavigationPanel_border_color, res.getColor(R.color.colorBorderOnBackground)));
            a.recycle();
        }
    }

    @Override
    public void onViewAdded(View child) {
        if(!(child instanceof NavIndicator)) {
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

        RelativeLayout.LayoutParams lp = (LayoutParams) view.getLayoutParams();
        double overflow = 0.4 * (lp.topMargin + lp.bottomMargin);
        int h = (int) (view.getHeight() + overflow);
        int t = (int) (view.getTop() - 0.5 * overflow);
        navIndicator.setIndicatorHeight(h);
        ObjectAnimator anim = ObjectAnimator.ofInt(navIndicator, "indicatorTop", t);
        anim.setDuration(200);
        anim.start();

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

    private class NavIndicator extends View {
        private int indicatorTop;
        private int indicatorHeight;

        private Rect tmpBorder;
        private Rect tmpIndicator;
        private Paint paintBorder;
        private Paint paintIndicator;

        public NavIndicator(Context context) {
            super(context);
            tmpBorder = new Rect(indicatorWidth - borderWidth, 0, indicatorWidth, 0);
            tmpIndicator = new Rect(0, 0, indicatorWidth, 0);

            paintBorder = new Paint();
            paintIndicator = new Paint();
        }

        public void setBorderColor(int color) {
            paintBorder.setColor(color);
        }

        public void setIndicatorColor(int color) {
            paintIndicator.setColor(color);
        }

        private void updateRects() {
            tmpIndicator.top = indicatorTop;
            tmpIndicator.bottom = indicatorTop + indicatorHeight;

            tmpBorder.bottom = getHeight();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            updateRects();
            canvas.drawRect(tmpBorder, paintBorder);
            canvas.drawRect(tmpIndicator, paintIndicator);
        }

        public void setIndicatorHeight(int height) {
            this.indicatorHeight = height;
        }

        public int getIndicatorTop() {
            return this.indicatorTop;
        }

        public void setIndicatorTop(int top) {
            this.indicatorTop = top;
            invalidate();
        }
    }

    private static int dpToPxSize(float density, int dps) {
        return (int) (dps * density + 0.5f);
    }
}
