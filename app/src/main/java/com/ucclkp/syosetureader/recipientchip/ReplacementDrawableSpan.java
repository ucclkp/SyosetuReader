package com.ucclkp.syosetureader.recipientchip;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.support.annotation.NonNull;
import android.text.style.ReplacementSpan;

/**
 * ReplacementSpan that properly draws the drawable that is centered around the text
 * without changing the default text size or layout.
 */
public class ReplacementDrawableSpan extends ReplacementSpan
{
    private int mTextColor;
    private int mFontHeight;
    private boolean mUseTextColor;
    private ShapeDrawable mDrawable;


    public ReplacementDrawableSpan(int bgColor, int textColor, boolean useTextColor)
    {
        super();

        mTextColor = textColor;
        mUseTextColor = useTextColor;

        mFontHeight = 0;
        mDrawable = new ShapeDrawable();
        mDrawable.getPaint().setColor(bgColor);
        mDrawable.getPaint().setAlpha(Color.alpha(bgColor));
    }


    @Override
    public int getSize(@NonNull Paint paint, CharSequence text,
                       int start, int end, Paint.FontMetricsInt fm)
    {
        int textWidth = (int) Math.floor(paint.measureText(text, start, end));
        int lineHeight;
        int spanHeight;

        if (fm != null)
        {
            paint.getFontMetricsInt(fm);

            lineHeight = fm.bottom - fm.top;
            mFontHeight = lineHeight;

            fm.top -= (int) Math.floor(lineHeight * 0.1f);
            fm.bottom += (int) Math.floor(lineHeight * 0.1f);
            fm.ascent = fm.top;
            fm.descent = fm.bottom;

            spanHeight = fm.bottom - fm.top;
        }
        else
        {
            Paint.FontMetricsInt fontMetrics
                    = paint.getFontMetricsInt();

            lineHeight = fontMetrics.bottom - fontMetrics.top;
            mFontHeight = lineHeight;

            int top = fontMetrics.top - (int) Math.floor(lineHeight * 0.1f);
            int bottom = fontMetrics.bottom + (int) Math.floor(lineHeight * 0.1f);

            spanHeight = bottom - top;
        }

        float[] radius = new float[8];
        for (int i = 0; i < radius.length; ++i)
            radius[i] = spanHeight / 8.f;

        mDrawable.setShape(new RoundRectShape(radius, null, null));
        mDrawable.setBounds(0, 0, textWidth + lineHeight, (int) (spanHeight * 0.9f));

        return mDrawable.getBounds().right;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence charSequence,
                     int start, int end, float x, int top,
                     int y, int bottom, @NonNull Paint paint)
    {
        canvas.save();
        float transY = (bottom - mDrawable.getBounds().bottom + top) / 2.f;
        canvas.translate(x, transY);
        mDrawable.draw(canvas);
        canvas.restore();

        if (mUseTextColor)
        {
            paint.setColor(mTextColor);
            paint.setAlpha(Color.alpha(mTextColor));
        }
        canvas.drawText(charSequence, start, end, x + mFontHeight / 2.f, y, paint);
    }

    protected Rect getBounds()
    {
        return mDrawable.getBounds();
    }
}
