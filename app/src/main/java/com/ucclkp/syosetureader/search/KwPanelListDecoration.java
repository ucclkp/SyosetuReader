package com.ucclkp.syosetureader.search;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

public class KwPanelListDecoration extends RecyclerView.ItemDecoration
{
    private int mDividerSpace;

    private int mDividerColor;
    private Paint mDividerPaint;


    public KwPanelListDecoration(Context context)
    {
        mDividerSpace = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1, context.getResources().getDisplayMetrics());

        TypedArray a = context.obtainStyledAttributes(
                new int[]{android.R.attr.colorControlNormal});
        mDividerColor = a.getColor(a.getIndex(0), Color.GRAY);
        a.recycle();

        mDividerPaint = new Paint();
        mDividerPaint.setStyle(Paint.Style.FILL);
        mDividerPaint.setColor(mDividerColor);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
    {
        super.getItemOffsets(outRect, view, parent, state);

        RecyclerView.ViewHolder holder
                = parent.findContainingViewHolder(view);
        if (holder != null)
        {
            int viewType = holder.getItemViewType();
            int position = holder.getAdapterPosition();

            if (position == RecyclerView.NO_POSITION)
                position = holder.getOldPosition();

            if (viewType == KwPanelListAdapter.TYPE_HEAD
                    && position > 0)
            {
                outRect.set(0, mDividerSpace, 0, 0);
            }
        }
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state)
    {
        super.onDraw(c, parent, state);
        //drawHorizontal(c, parent);
    }

    private void drawHorizontal(Canvas canvas, RecyclerView parent)
    {
        final int left = parent.getPaddingLeft();
        final int right = parent.getMeasuredWidth() - parent.getPaddingRight();
        final int childSize = parent.getChildCount();
        for (int i = 0; i < childSize - 1; ++i)
        {
            final View child = parent.getChildAt(i);

            RecyclerView.ViewHolder holder
                    = parent.findContainingViewHolder(child);
            if (holder != null)
            {
                int viewType = holder.getItemViewType();
                if (viewType == KwPanelListAdapter.TYPE_HEAD)
                {
                    int position = holder.getAdapterPosition();

                    if (position == RecyclerView.NO_POSITION)
                        position = holder.getOldPosition();

                    if (position > 0)
                    {
                        RecyclerView.LayoutParams layoutParams
                                = (RecyclerView.LayoutParams) child.getLayoutParams();
                        final int top = child.getTop() - layoutParams.topMargin - mDividerSpace;
                        final int bottom = top + mDividerSpace;

                        canvas.drawRect(left, top, right, bottom, mDividerPaint);
                    }
                }
            }
        }
    }
}
