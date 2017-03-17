package com.ucclkp.syosetureader.statictextview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.ucclkp.syosetureader.R;

class TextHandle
{
    private int mWidth;
    private int mHeight;
    private int mWindowX;
    private int mWindowY;
    private HandleType mHandleType;
    private PopupWindow mWindow;

    private float mStartX;
    private float mStartY;
    private boolean mIsTouchDown;
    private boolean mIsTouchMoved;

    private HandleListener mHandleListener;


    enum HandleType
    {
        LEFT,
        RIGHT,
    }


    TextHandle(Context context, HandleType type)
    {
        mWindowX = -1;
        mWindowY = -1;
        mHandleType = type;

        mIsTouchDown = false;
        mIsTouchMoved = false;

        Drawable drawable = null;
        switch (type)
        {
            case LEFT:
                drawable = context.getDrawable(
                        R.drawable.text_select_handle_left);
                break;

            case RIGHT:
                drawable = context.getDrawable(
                        R.drawable.text_select_handle_right);
                break;
        }

        if (drawable != null)
        {
            mWidth = drawable.getIntrinsicWidth();
            mHeight = drawable.getIntrinsicHeight();

            drawable.setBounds(0, 0, mWidth, mHeight);
        }

        mWindow = new PopupWindow(context);
        mWindow.setWidth(mWidth);
        mWindow.setHeight(mHeight);
        mWindow.setContentView(new View(context));
        mWindow.setClippingEnabled(false);
        mWindow.setSplitTouchEnabled(true);
        mWindow.setBackgroundDrawable(drawable);
        mWindow.setTouchInterceptor(mTouchListener);
        mWindow.setAnimationStyle(R.style.TextAssistWindowAnimation);
    }


    public void setHandleEventListener(HandleListener l)
    {
        mHandleListener = l;
    }


    public int getX()
    {
        return mWindowX;
    }

    public int getY()
    {
        return mWindowY;
    }

    public int getWidth()
    {
        return mWidth;
    }

    public int getHeight()
    {
        return mHeight;
    }

    public HandleType getType()
    {
        return mHandleType;
    }


    //forceUpdate: 为true时，将立即更新窗口位置。
    public void show(View parent, int x, int y, boolean forceUpdate)
    {
        mWindowX = x;
        mWindowY = y;

        if (isDragging() || forceUpdate)
        {
            if (mWindow.isShowing())
                mWindow.update(x, y, -1, -1);
            else
                mWindow.showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        }
        else
        {
            if (mWindow.isShowing())
                mWindow.dismiss();

            mWindow.showAtLocation(parent, Gravity.NO_GRAVITY, x, y);
        }
    }

    void close()
    {
        mWindow.dismiss();
    }

    boolean isShowing()
    {
        return mWindow.isShowing();
    }

    boolean isDragging()
    {
        return mIsTouchMoved;
    }


    private View.OnTouchListener mTouchListener = new View.OnTouchListener()
    {
        @Override
        public boolean onTouch(View v, MotionEvent event)
        {
            switch (event.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                    mIsTouchDown = true;
                    mIsTouchMoved = false;
                    mStartX = event.getRawX();
                    mStartY = event.getRawY();
                    if (mHandleListener != null)
                        mHandleListener.onCapture(TextHandle.this, event);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mIsTouchDown)
                    {
                        float curRawX = event.getRawX();
                        float curRawY = event.getRawY();

                        float slope = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 4,
                                v.getContext().getResources().getDisplayMetrics());

                        boolean canTouchMove = mIsTouchMoved ||
                                (Math.abs(curRawX - mStartX) > slope
                                        || Math.abs(curRawY - mStartY) > slope);

                        if (canTouchMove)
                        {
                            mIsTouchMoved = true;
                            if (mHandleListener != null)
                                mHandleListener.onDrag(TextHandle.this, event);
                        }
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (!mIsTouchMoved)
                    {
                        if (mHandleListener != null)
                            mHandleListener.onClick(TextHandle.this, event);
                    }

                    if (mHandleListener != null)
                        mHandleListener.onRelease(TextHandle.this, event);

                    mIsTouchDown = false;
                    mIsTouchMoved = false;
                    break;
            }

            return true;
        }
    };


    interface HandleListener
    {
        void onCapture(TextHandle view, MotionEvent e);

        void onDrag(TextHandle view, MotionEvent e);

        void onClick(TextHandle view, MotionEvent e);

        void onRelease(TextHandle view, MotionEvent e);
    }
}
