package com.ucclkp.syosetureader;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Html;

public class SyosetuImageGetter implements Html.ImageGetter
{
    private String mPageId;
    private Context mContext;
    private SyosetuCacheManager mCacheMgr;

    private static final int IMAGE_RECOMMEND_HEIGHT = 400;


    public SyosetuImageGetter(Context context)
    {
        mPageId = null;
        mContext = context;
        mCacheMgr = ((UApplication) ((Activity) context).getApplication())
                .getCacheManager();
    }

    public SyosetuImageGetter(Context context, String pageId)
    {
        mPageId = pageId;
        mContext = context;
        mCacheMgr = ((UApplication) ((Activity) context).getApplication())
                .getCacheManager();
    }


    @Override
    public Drawable getDrawable(String source)
    {
        UrlDrawable urlDrawable;

        Bitmap bmp = mCacheMgr.getBitmap(source);
        if (bmp != null)
        {
            Drawable drawable = new BitmapDrawable(mContext.getResources(), bmp);

            int height = IMAGE_RECOMMEND_HEIGHT;
            float factor = (float) height / drawable.getIntrinsicHeight();
            int width = (int) (drawable.getIntrinsicWidth() * factor);
            drawable.setBounds(0, 0, width, height);

            urlDrawable = new UrlDrawable(mContext.getResources(), null);
            urlDrawable.mSource = source;
            urlDrawable.mDrawable = drawable;
            urlDrawable.mState = UrlDrawable.State.STATE_COMPLETED;
            urlDrawable.setBounds(drawable.getBounds());
        } else
        {
            Drawable drawable = new BlankDrawable(1, IMAGE_RECOMMEND_HEIGHT);
            drawable.setBounds(0, 0,
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());

            urlDrawable = new UrlDrawable(mContext.getResources(), null);
            urlDrawable.mSource = source;
            urlDrawable.mDrawable = drawable;
            urlDrawable.setBounds(drawable.getBounds());

            UApplication.imageDownloader.download(mPageId, source);
        }

        return urlDrawable;
    }


    private class BlankDrawable extends ColorDrawable
    {
        private int mWidth;
        private int mHeight;

        public BlankDrawable(int width, int height)
        {
            super(Color.TRANSPARENT);

            mWidth = width;
            mHeight = height;
        }

        @Override
        public int getIntrinsicWidth()
        {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight()
        {
            return mHeight;
        }
    }
}