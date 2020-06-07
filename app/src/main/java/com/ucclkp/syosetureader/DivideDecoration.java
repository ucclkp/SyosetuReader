package com.ucclkp.syosetureader;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;

public class DivideDecoration extends RecyclerView.ItemDecoration
{
    private int mDividerSize;
    private int mDividerColor;
    private Paint mDividerPaint;


    public DivideDecoration(Context context)
    {
        mDividerSize = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1,
                context.getResources().getDisplayMetrics());

        mDividerColor = ContextCompat.getColor(context, R.color.divider_color);

        mDividerPaint = new Paint();
        mDividerPaint.setStyle(Paint.Style.FILL);
        mDividerPaint.setColor(mDividerColor);
        mDividerPaint.setAlpha(Color.alpha(mDividerColor));
    }


    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
    {
        super.getItemOffsets(outRect, view, parent, state);

        RecyclerView.ViewHolder holder
                = parent.findContainingViewHolder(view);
        if (holder != null)
        {
            int position = holder.getAdapterPosition();

            if (position == RecyclerView.NO_POSITION)
                position = holder.getOldPosition();

            if (position != state.getItemCount() - 1)
                outRect.set(0, 0, 0, mDividerSize);
            else
                outRect.set(0, 0, 0, 0);
        }
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state)
    {
        super.onDraw(c, parent, state);
        drawVerticalLine(c, parent, state);
    }


    private void drawVerticalLine(Canvas c, RecyclerView parent, RecyclerView.State state)
    {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++)
        {
            final View child = parent.getChildAt(i);
            RecyclerView.ViewHolder holder
                    = parent.findContainingViewHolder(child);
            if (holder != null)
            {
                int position = holder.getAdapterPosition();

                if (position == RecyclerView.NO_POSITION)
                    position = holder.getOldPosition();

                if (position != state.getItemCount() - 1)
                {
                    RecyclerView.LayoutParams params
                            = (RecyclerView.LayoutParams) child.getLayoutParams();
                    int top = child.getBottom() + params.bottomMargin
                            + (int) Math.floor(child.getTranslationY());
                    int bottom = top + mDividerSize;

                    int alpha = Color.alpha(mDividerColor);
                    mDividerPaint.setAlpha((int) ((child.getAlpha() / 1.f) * alpha));

                    c.drawRect(left + child.getTranslationX(), top,
                            right + child.getTranslationX(), bottom,
                            mDividerPaint);
                }
            }
        }
    }
}