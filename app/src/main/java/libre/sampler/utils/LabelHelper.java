package libre.sampler.utils;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

public class LabelHelper {
    private LabelHelper() {}

    public interface LabelSelector {
        void setup(int index, TextView outLabel);
    }

    public static void refreshSegmented(LinearLayout labelContainer, int startIndex, int count,
                                        @LayoutRes int labelResId, LabelSelector selector) {
        int childCount = labelContainer.getChildCount();

        LayoutInflater inflater = LayoutInflater.from(labelContainer.getContext());
        for(int i = 0; i < count; i++) {
            int viewIndex = i + startIndex;

            TextView v;
            boolean willInflate = (viewIndex >= childCount);
            if(willInflate) {
                v = (TextView) inflater.inflate(labelResId, labelContainer, false);
            } else {
                v = (TextView) labelContainer.getChildAt(viewIndex);
            }

            selector.setup(i, v);

            if(willInflate) {
                labelContainer.addView(v, viewIndex);
            } else {
                v.requestLayout();
            }
        }
        while(childCount > count + startIndex) {
            labelContainer.removeViewAt(childCount - 1);
            childCount--;
        }
    }

    public static int getLabelSize(double segmentSize, int i) {
        double fractionalSegmentSize = segmentSize % 1.0f;
        int labelSize = (int) segmentSize;
        double existingFractional = fractionalSegmentSize * i;
        if(Math.round(existingFractional + fractionalSegmentSize) > Math.round(existingFractional)) {
            labelSize += 1;
        }

        return labelSize;
    }
}
