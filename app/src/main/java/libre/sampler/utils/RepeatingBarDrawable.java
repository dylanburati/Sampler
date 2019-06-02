package libre.sampler.utils;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.appcompat.graphics.drawable.DrawableWrapper;

public class RepeatingBarDrawable extends DrawableWrapper {
    private int repeatDirection;
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    public RepeatingBarDrawable(Drawable drawable, int direction) {
        super(drawable);
        repeatDirection = direction;
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        Drawable drawable = getWrappedDrawable();

        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        boolean repeatX = (repeatDirection & HORIZONTAL) != 0;
        boolean repeatY = (repeatDirection & VERTICAL) != 0;
        int tilesX = repeatX ? 1 + (bounds.width() / width) : 1;
        int tilesY = repeatY ? 1 + (bounds.height() / height) : 1;

        for(int ix = 0; ix < tilesX; ix++) {
            for(int iy = 0; iy < tilesY; iy++) {
                int tileLeft = bounds.left + ix * width;
                int tileTop = bounds.top + iy * height;
                drawable.setBounds(tileLeft, tileTop, tileLeft + width, tileTop + height);
                drawable.draw(canvas);
            }
        }
    }
}
