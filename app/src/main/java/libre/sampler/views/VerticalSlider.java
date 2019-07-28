/*
 * Copyright (C) 2017 The Android Open Source Project
 * Modifications Copyright (C) 2019 Dylan Burati
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libre.sampler.views;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import java.util.concurrent.CopyOnWriteArrayList;

import androidx.annotation.Nullable;
import libre.sampler.R;
import static libre.sampler.utils.ViewUtil.*;

public class VerticalSlider extends View {
    public static final int DEFAULT_BAR_WIDTH_DP = 4;
    public static final int DEFAULT_THUMB_WIDTH_DP = 12;
    public static final int DEFAULT_THUMB_PRESSED_WIDTH_DP = 16;
    public static final int DEFAULT_TOUCH_TARGET_WIDTH_DP = 26;
    public static final int DEFAULT_PADDING_TOP_DP = 8;

    private Rect seekBounds;
    private Rect progressBar;
    private Rect backgroundBar;
    private Paint progressPaint;
    private Paint backgroundPaint;
    private Paint thumbPaint;
    private Drawable thumbDrawable;

    private boolean isDragging;
    private int position;
    private int maxPosition;
    private CopyOnWriteArrayList<OnProgressChangedListener> listeners;
    private float lastProgress = -1;

    private int barWidth;
    private int touchTargetWidth;
    private int thumbWidth;
    private int thumbPressedWidth;
    private int thumbPadding;
    private int paddingTop;

    private int[] tempLocationOnScreen = new int[2];
    private Point tempTouchPosition = new Point();

    public VerticalSlider(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        seekBounds = new Rect();
        progressBar = new Rect();
        backgroundBar = new Rect();
        progressPaint = new Paint();
        backgroundPaint = new Paint();
        thumbPaint = new Paint();
        listeners = new CopyOnWriteArrayList<>();

        Resources res = context.getResources();
        DisplayMetrics displayMetrics = res.getDisplayMetrics();
        float density = displayMetrics.density;
        int defaultBarWidth = dpToPxSize(density, DEFAULT_BAR_WIDTH_DP);
        int defaultTouchTargetWidth = dpToPxSize(density, DEFAULT_TOUCH_TARGET_WIDTH_DP);
        int defaultThumbWidth = dpToPxSize(density, DEFAULT_THUMB_WIDTH_DP);
        int defaultThumbPressedWidth = dpToPxSize(density, DEFAULT_THUMB_PRESSED_WIDTH_DP);
        int defaultPaddingTop = dpToPxSize(density, DEFAULT_PADDING_TOP_DP);
        if(attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.VerticalSlider, 0, 0);
            try {
                thumbDrawable = a.getDrawable(R.styleable.VerticalSlider_thumb_drawable);
                if(thumbDrawable != null) {
                    defaultTouchTargetWidth = Math.max(thumbDrawable.getMinimumWidth(), defaultTouchTargetWidth);
                }
                barWidth = a.getDimensionPixelSize(R.styleable.VerticalSlider_bar_width, defaultBarWidth);
                touchTargetWidth = a.getDimensionPixelSize(R.styleable.VerticalSlider_touch_target_width, defaultTouchTargetWidth);
                thumbWidth = a.getDimensionPixelSize(R.styleable.VerticalSlider_thumb_width, defaultThumbWidth);
                thumbPressedWidth = a.getDimensionPixelSize(R.styleable.VerticalSlider_thumb_pressed_width, defaultThumbPressedWidth);
                paddingTop = a.getDimensionPixelSize(R.styleable.VerticalSlider_paddingTop, defaultPaddingTop);
                int backgroundColor = a.getColor(R.styleable.VerticalSlider_background_color, Color.BLACK);
                int progressColor = a.getColor(R.styleable.VerticalSlider_progress_color, Color.WHITE);
                int thumbColor = a.getColor(R.styleable.VerticalSlider_thumb_color, Color.WHITE);
                backgroundPaint.setColor(backgroundColor);
                progressPaint.setColor(progressColor);
                thumbPaint.setColor(thumbColor);
            } finally {
                a.recycle();
            }
        } else {
            barWidth = defaultBarWidth;
            touchTargetWidth = defaultTouchTargetWidth;
            thumbWidth = defaultThumbWidth;
            thumbPressedWidth = defaultThumbPressedWidth;
            backgroundPaint.setColor(Color.BLACK);
            progressPaint.setColor(Color.WHITE);
            thumbPaint.setColor(Color.WHITE);
            paddingTop = defaultPaddingTop;
        }

        setPadding(0, paddingTop, 0, 0);
        if(thumbDrawable != null) {
            thumbPadding = (thumbDrawable.getMinimumWidth() + 1) / 2;
        } else {
            thumbPadding = (Math.max(thumbWidth, thumbPressedWidth) + 1) / 2;
        }
        setFocusable(true);
        if(getImportantForAccessibility() == View.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.save();
        drawBackgroundBar(canvas);
        drawProgress(canvas);
        canvas.restore();
    }

    private void drawBackgroundBar(Canvas canvas) {
        int barWidth = backgroundBar.width();
        int barLeft = backgroundBar.centerX() - barWidth / 2;
        int barRight = barLeft + barWidth;
        canvas.drawRect(barLeft, backgroundBar.top, barRight, backgroundBar.bottom, backgroundPaint);
    }

    private void drawProgress(Canvas canvas) {
        if(maxPosition <= 0) {
            return;
        }
        int barWidth = backgroundBar.width();
        int barLeft = backgroundBar.centerX() - barWidth / 2;
        int barRight = barLeft + barWidth;
        canvas.drawRect(barLeft, progressBar.top, barRight, progressBar.bottom, progressPaint);

        if (thumbDrawable == null) {
            int thumbW = (isDragging || isFocused()) ? thumbPressedWidth : thumbWidth;
            int thumbRadius = thumbW / 2;
            canvas.drawCircle(backgroundBar.centerX(), progressBar.top, thumbRadius, thumbPaint);
        } else {
            int thumbDrawableWidth = thumbDrawable.getIntrinsicWidth();
            int thumbDrawableHeight = thumbDrawable.getIntrinsicHeight();
            thumbDrawable.setBounds(
                    backgroundBar.centerX() - thumbDrawableWidth / 2,
                    progressBar.top - thumbDrawableHeight / 2,
                    backgroundBar.centerX() + thumbDrawableWidth / 2,
                    progressBar.top + thumbDrawableHeight / 2);
            thumbDrawable.draw(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled() || maxPosition <= 0) {
            return false;
        }
        Point touchPosition = resolveRelativeTouchPosition(event);
        int x = touchPosition.x;
        int y = touchPosition.y;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(seekBounds.contains(x, y)) {
                    startDragging();
                    setThumbPosition(y);
                    updateView();
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(isDragging) {
                    setThumbPosition(y);
                    updateView();
                    invalidate();
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(isDragging) {
                    stopDragging();
                    return true;
                }
                break;
            default:
                // Do nothing.
        }
        return false;
    }

    private Point resolveRelativeTouchPosition(MotionEvent motionEvent) {
        getLocationOnScreen(tempLocationOnScreen);
        tempTouchPosition.set(((int) motionEvent.getRawX()) - tempLocationOnScreen[0],
                ((int) motionEvent.getRawY()) - tempLocationOnScreen[1]);
        return tempTouchPosition;
    }

    @Override
    protected void onFocusChanged(
            boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if(isDragging && !gainFocus) {
            stopDragging();
        }
    }

    private void startDragging() {
        isDragging = true;
        setPressed(true);
        ViewParent parent = getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    private void stopDragging() {
        isDragging = false;
        setPressed(false);
        ViewParent parent = getParent();
        if(parent != null) {
            parent.requestDisallowInterceptTouchEvent(false);
        }
        updateView();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    private void updateDrawableState() {
        if (thumbDrawable != null && thumbDrawable.isStateful() && thumbDrawable.setState(getDrawableState())) {
            invalidate();
        }
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (thumbDrawable != null) {
            thumbDrawable.jumpToCurrentState();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int width = (widthMode == MeasureSpec.UNSPECIFIED) ? touchTargetWidth
                : (widthMode == MeasureSpec.EXACTLY) ? widthSize
                : /* widthMode == MeasureSpec.AT_MOST */ Math.min(touchTargetWidth, widthSize);
        setMeasuredDimension(width, MeasureSpec.getSize(heightMeasureSpec));
        updateDrawableState();
    }
    
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        int barX = (width - touchTargetWidth) / 2;
        int seekBottom = height - getPaddingBottom();
        int seekTop = getPaddingTop();

        int progressX = barX + (touchTargetWidth - barWidth) / 2;
        seekBounds.set(barX, seekTop, barX + touchTargetWidth, seekBottom);
        backgroundBar.set(progressX, seekBounds.top + thumbPadding,
                progressX + barWidth, seekBounds.bottom - thumbPadding);

        if(maxPosition > 0) {
            position = position * backgroundBar.height() / maxPosition;
        }
        maxPosition = backgroundBar.height();
        updateView();
    }

    private void setThumbPosition(int y) {
        this.position = Math.max(0, Math.min(maxPosition, backgroundBar.bottom - y));
        dispatchProgressChanged(true);
    }

    // Called from controller code
    public void setProgress(float progress) {
        this.position = Math.round(progress * maxPosition);
        updateView();
        dispatchProgressChanged(progress, false);
    }

    private void updateView() {
        progressBar.set(backgroundBar);
        if(maxPosition > 0) {
            progressBar.top = backgroundBar.bottom - position;
        }
        invalidate();
    }

    public void addListener(OnProgressChangedListener fn) {
        listeners.add(fn);
    }

    public void removeListener(OnProgressChangedListener fn) {
        listeners.remove(fn);
    }

    private void dispatchProgressChanged(boolean fromUser) {
        float progress = position / (float) maxPosition;
        dispatchProgressChanged(progress, fromUser);
    }

    private void dispatchProgressChanged(float progress, boolean fromUser) {
        if(Math.abs(progress - lastProgress) > 1e-6) {
            lastProgress = progress;
            for(OnProgressChangedListener fn : listeners) {
                fn.onProgressChanged(this, progress, fromUser);
            }
        }
    }

    public interface OnProgressChangedListener {
        void onProgressChanged(VerticalSlider v, float progress, boolean fromUser);
    }
}
