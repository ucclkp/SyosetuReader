package com.ucclkp.syosetureader.search;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class KeywordBar extends LinearLayout
{
    private int mSelection;
    private int mIndicatorHeight;
    private int mBorderHeight;
    private Paint mIndicatorPaint;

    private int mIndicatorColor;
    private int mTextNormalColor;

    private OnItemSelectListener mListener;


    public final static int NO_POSITION = -1;


    public KeywordBar(Context context)
    {
        this(context, null);
    }

    public KeywordBar(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public KeywordBar(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);

        setWillNotDraw(false);
        setOrientation(HORIZONTAL);

        mIndicatorHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 3,
                context.getResources().getDisplayMetrics());

        mBorderHeight = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1,
                context.getResources().getDisplayMetrics());

        TypedArray a = context.obtainStyledAttributes(
                new int[]{android.R.attr.colorAccent, android.R.attr.textColorPrimary});
        mIndicatorColor = a.getColor(a.getIndex(0), Color.BLACK);
        mTextNormalColor = a.getColor(a.getIndex(1), Color.BLACK);
        a.recycle();

        mSelection = NO_POSITION;

        mIndicatorPaint = new Paint();
        mIndicatorPaint.setStyle(Paint.Style.FILL);
        mIndicatorPaint.setColor(mIndicatorColor);
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        int pBottom = getBottom();
        canvas.drawRect(
                getLeft(), pBottom - mBorderHeight, getRight(), pBottom,
                mIndicatorPaint);

        if (mSelection >= 0)
        {
            View child = getChildAt(mSelection);
            if (child != null)
            {
                int bottom = child.getBottom();
                canvas.drawRect(
                        child.getLeft(), bottom - mIndicatorHeight, child.getRight(), bottom,
                        mIndicatorPaint);
            }
        }
    }

    @Override
    public void onViewAdded(View child)
    {
        super.onViewAdded(child);
        child.setOnClickListener(mItemClickListener);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b)
    {
        super.onLayout(changed, l, t, r, b);
    }

    public void setSelection(int position)
    {
        if (position != NO_POSITION
                && (position < 0 || position > getChildCount() - 1))
            return;

        if (mSelection != position)
        {
            if (mSelection >= 0 && mSelection <= getChildCount() - 1)
            {
                TextView unselectedChild = (TextView) getChildAt(mSelection);
                unselectedChild.setTextColor(mTextNormalColor);
            }

            mSelection = position;

            TextView selectedChild = (TextView) getChildAt(position);
            selectedChild.setTextColor(mIndicatorColor);

            invalidate();

            if (mListener != null)
                mListener.onItemSelected(mSelection);
        }
    }

    public int getSelection()
    {
        return mSelection;
    }

    public void setOnItemSelectListener(OnItemSelectListener l)
    {
        mListener = l;
    }


    private int getChildPosition(View child)
    {
        for (int i = 0; i < getChildCount(); ++i)
        {
            if (getChildAt(i) == child)
                return i;
        }

        return -1;
    }


    private OnClickListener mItemClickListener = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            setSelection(getChildPosition(v));
        }
    };


    public interface OnItemSelectListener
    {
        void onItemSelected(int position);
    }
}