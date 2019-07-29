package libre.sampler.utils;

import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.LayoutRes;

public class LabelHelper {
    private LabelHelper() {}

    public static void refreshSegmented(LinearLayout labelContainer, double segmentSize, int startIndex,
                                        @LayoutRes int labelResId) {
        int availableSize = (labelContainer.getOrientation() == LinearLayout.HORIZONTAL ?
                labelContainer.getLayoutParams().width : labelContainer.getLayoutParams().height);
        String[] labels = new String[(int) (availableSize / segmentSize) + 1];
        for(int i = 0; i < labels.length; i++) {
            labels[i] = String.format("%d", i);
        }
        refreshSegmented(labelContainer, segmentSize, startIndex, labels, labelResId);
    }

    public static void refreshSegmented(LinearLayout labelContainer, double segmentSize, int startIndex,
                                        @NotNull String[] labels, @LayoutRes int labelResId) {
        if(segmentSize < 1) {
            return;
        }

        int childCount = labelContainer.getChildCount();

        LayoutInflater inflater = LayoutInflater.from(labelContainer.getContext());
        for(int i = 0; i < labels.length; i++) {
            int viewIndex = i + startIndex;
            String label;
            label = labels[i];

            TextView v;
            boolean willInflate = (viewIndex >= childCount);
            if(willInflate) {
                v = (TextView) inflater.inflate(labelResId, labelContainer, false);
            } else {
                v = (TextView) labelContainer.getChildAt(viewIndex);
            }

            if(labelContainer.getOrientation() == LinearLayout.HORIZONTAL) {
                v.getLayoutParams().width = getLabelSize(segmentSize, i);
            } else {
                v.getLayoutParams().height = getLabelSize(segmentSize, i);
            }
            v.requestLayout();

            if(willInflate) {
                labelContainer.addView(v, viewIndex);
            }
            v.setText(label);
        }
        while(childCount > labels.length + startIndex) {
            labelContainer.removeViewAt(childCount - 1);
            childCount--;
        }
    }

    private static int getLabelSize(double segmentSize, int i) {
        double fractionalSegmentSize = segmentSize % 1.0f;
        int labelSize = (int) segmentSize;
        double existingFractional = fractionalSegmentSize * i;
        if(Math.round(existingFractional + fractionalSegmentSize) > Math.round(existingFractional)) {
            labelSize += 1;
        }

        return labelSize;
    }
}
