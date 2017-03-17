package com.ucclkp.syosetureader;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class UrlDrawable extends BitmapDrawable
{
    public State mState;
    public String mSource;
    public Drawable mDrawable;


    public enum State
    {
        STATE_DOWNLOADING,
        STATE_COMPLETED
    }


    public UrlDrawable(Resources res, Bitmap bitmap)
    {
        super(res, bitmap);

        mState = State.STATE_DOWNLOADING;
    }


    @Override
    public void draw(Canvas canvas)
    {
        if (mDrawable != null)
        {
            mDrawable.draw(canvas);
        }
    }
}
