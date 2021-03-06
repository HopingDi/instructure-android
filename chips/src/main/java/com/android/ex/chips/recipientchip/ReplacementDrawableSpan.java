package com.android.ex.chips.recipientchip;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * ReplacementSpan that properly draws the drawable that is centered around the text
 * without changing the default text size or layout.
 */
public class ReplacementDrawableSpan extends ReplacementSpan {
    protected Drawable mDrawable;
    private final Paint mWorkPaint = new Paint();
    private float mExtraMargin;

    public ReplacementDrawableSpan(Drawable drawable) {
        super();
        mDrawable = drawable;
    }

    public void setExtraMargin(float margin) {
        mExtraMargin = margin;
    }

    private void setupFontMetrics(Paint.FontMetricsInt fm, Paint paint) {
        mWorkPaint.set(paint);
        if (fm != null) {
            mWorkPaint.getFontMetricsInt(fm);

            final Rect bounds = getBounds();
            final int textHeight = fm.descent - fm.ascent;
            final int halfMargin = (int) mExtraMargin / 2;
            fm.ascent = Math.min(fm.top, fm.top + (textHeight - bounds.bottom) / 2) - halfMargin;
            fm.descent = Math.max(fm.bottom, fm.bottom + (bounds.bottom - textHeight) / 2)
                    + halfMargin;
            fm.top = fm.ascent;
            fm.bottom = fm.descent;
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text, int i, int i2, Paint.FontMetricsInt fm) {
        setupFontMetrics(fm, paint);
        return getBounds().right;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence charSequence, int start, int end, float x, int top,
                     int y, int bottom, @NonNull Paint paint) {
        canvas.save();
        int transY = (bottom - mDrawable.getBounds().bottom + top) / 2;
        canvas.translate(x, transY);
        mDrawable.draw(canvas);
        canvas.restore();
    }

    protected Rect getBounds() {
        return mDrawable.getBounds();
    }
}
