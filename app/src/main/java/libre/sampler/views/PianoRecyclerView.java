package libre.sampler.views;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import libre.sampler.R;
import libre.sampler.utils.Debouncer;

public class PianoRecyclerView extends androidx.recyclerview.widget.RecyclerView {
    private OnTouchListener scaleGestureDetector;
    private OnZoomListener listener;

    public PianoRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initScaleDetector();
    }

    public PianoRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initScaleDetector();
    }

    public void setZoomListener(OnZoomListener listener) {
        this.listener = listener;
    }

    private float scrollBarHeight;
    private void initScaleDetector() {
        scrollBarHeight = getResources().getDimension(R.dimen.text_caption) + getResources().getDimension(R.dimen.margin2) * 3;
        this.scaleGestureDetector = new OnTouchListener() {
            private final Debouncer debouncer = new Debouncer();

            private boolean inProgress = false;
            private int numPointersTracking = 0;
            private int[] trackingPointerIds = new int[2];

            private float initialSpan = 1.0f;
            private float beginScaleFactor = 1.0f;
            private float lastDispatchedScaleFactor = 1.0f;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                int pointerCount = e.getPointerCount();

                int[] pointersInScrollbar = new int[2];
                float[] locationsInScrollbar = new float[2];
                int arrIndex = 0;
                for(int i = 0; arrIndex < 2 && i < pointerCount; i++) {
                    if(e.getY(i) <= scrollBarHeight) {
                        pointersInScrollbar[arrIndex] = e.getPointerId(i);
                        locationsInScrollbar[arrIndex] = e.getX(i);
                        arrIndex++;
                    }
                }
                if(inProgress) {
                    if(arrIndex < 2) {
                        inProgress = false;
                        trackingPointerIds[0] = -1;
                        trackingPointerIds[1] = -1;
                    } else if(pointersInScrollbar[0] == trackingPointerIds[0] && pointersInScrollbar[1] == trackingPointerIds[1]) {
                        // Same gesture
                        float span = locationsInScrollbar[1] - locationsInScrollbar[0];
                        float scaleFactor = span / initialSpan * beginScaleFactor;
                        scaleFactor = Math.min(5.0f, Math.max(0.2f, scaleFactor));
                        float focusX = 0.5f * (locationsInScrollbar[0] + locationsInScrollbar[1]) - getLeft();

                        Log.d("zoom listener", String.format("move span=[%.2f, %.2f]",
                                locationsInScrollbar[0], locationsInScrollbar[1]));
                        if(listener != null && Math.abs(scaleFactor - lastDispatchedScaleFactor) > 0.003) {
                            if(listener.onZoom(PianoRecyclerView.this, scaleFactor, focusX)) {
                                lastDispatchedScaleFactor = scaleFactor;
                            }
                        }
                    } else {
                        inProgress = false;  // New gesture will be started below
                    }
                }
                if(!inProgress && arrIndex == 2) {
                    trackingPointerIds = pointersInScrollbar;
                    initialSpan = locationsInScrollbar[1] - locationsInScrollbar[0];
                    beginScaleFactor = lastDispatchedScaleFactor;
                    inProgress = true;
                    Log.d("zoom listener", String.format("begin span=[%.2f, %.2f]",
                            locationsInScrollbar[0], locationsInScrollbar[1]));
                }

                return inProgress;
            }
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        scaleGestureDetector.onTouch(this, e);
        return super.onTouchEvent(e);
    }

    public interface OnZoomListener {
        boolean onZoom(RecyclerView rv, float scaleFactor, float focusX);
    }
}
