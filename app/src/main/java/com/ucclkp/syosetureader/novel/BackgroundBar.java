package com.ucclkp.syosetureader.novel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.ucclkp.syosetureader.R;

import java.util.ArrayList;

public class BackgroundBar extends LinearLayout
{
    private int mThumbCorner;
    private int mBorderWidth;

    private int mSelectedThumb;

    private Paint mBorderPaint;
    private ArrayList<Thumb> mThumbList;
    private OnSelectedItemChangedListener mListener;


    private static class Thumb
    {
        String name;
        Drawable drawable;
    }


    public BackgroundBar(Context context)
    {
        this(context, null);
    }

    public BackgroundBar(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public BackgroundBar(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        setWillNotDraw(false);
        setOrientation(HORIZONTAL);

        mSelectedThumb = -1;
        mThumbList = new ArrayList<>();

        mThumbCorner = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        mBorderWidth = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());

        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(ContextCompat.getColor(context, R.color.border_color_thumb));
    }

    @Override
    public void onDrawForeground(Canvas canvas)
    {
        super.onDrawForeground(canvas);

        if (mSelectedThumb >= 0 && mSelectedThumb < getChildCount())
        {
            View child = getChildAt(mSelectedThumb);
            canvas.drawRoundRect(
                    child.getLeft(), child.getTop(),
                    child.getRight(), child.getBottom(),
                    mThumbCorner, mThumbCorner, mBorderPaint);
        }
    }


    public void addThumb(String name)
    {
        addThumb(name, false);
    }

    public void addThumb(String name, boolean asDefault)
    {
        Thumb thumb = new Thumb();
        thumb.name = name;
        thumb.drawable = FormatDialogFragment.getBackgroundById(getContext(), name);
        mThumbList.add(thumb);

        CardView cardView = (CardView) LayoutInflater.from(getContext())
                .inflate(R.layout.button_format_thumb, this, false);
        cardView.setForeground(thumb.drawable);
        cardView.setOnClickListener(mThumbClickListener);

        if (asDefault)
        {
            mSelectedThumb = mThumbList.size() - 1;
            cardView.findViewById(R.id.tv_format_thumb_desc).setVisibility(VISIBLE);
            invalidate();
        }

        addView(cardView);
    }

    public void selectThumb(String name)
    {
        for (int i = 0; i < mThumbList.size(); ++i)
        {
            if (mThumbList.get(i).name.equals(name))
            {
                if (mSelectedThumb != i)
                {
                    mSelectedThumb = i;
                    invalidate();
                }
                break;
            }
        }
    }

    public void removeThumb(String name)
    {
        for (int i = 0; i < mThumbList.size(); )
        {
            Thumb thumb = mThumbList.get(i);
            if (thumb.name.equals(name))
            {
                mThumbList.remove(i);
                removeView(getChildAt(i));
            } else
                ++i;
        }
    }

    public void removeAllThumb()
    {
        mThumbList.clear();
        removeAllViews();
    }


    public void setOnSelectedItemChangedListener(OnSelectedItemChangedListener l)
    {
        mListener = l;
    }


    private OnClickListener mThumbClickListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            for (int i = 0; i < getChildCount(); ++i)
            {
                if (getChildAt(i) == v)
                {
                    if (mSelectedThumb != i
                            && mListener != null)
                        mListener.onSelectedItemChanged(i, mThumbList.get(i).drawable, mThumbList.get(i).name);

                    mSelectedThumb = i;
                    invalidate();
                    return;
                }
            }
        }
    };


    public interface OnSelectedItemChangedListener
    {
        void onSelectedItemChanged(int position, Drawable drawable, String name);
    }
}