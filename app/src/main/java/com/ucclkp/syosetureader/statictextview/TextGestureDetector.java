package com.ucclkp.syosetureader.statictextview;

import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;

class TextGestureDetector
{
    private float mPrevX;
    private float mPrevY;
    private float mStartX;
    private float mStartY;

    private boolean mIsTouchMoved;
    private boolean mIsLongPressed;

    private boolean mLongTappingResult;

    private boolean mIsLongTouchable;

    private int mActivePointerId;
    private boolean mIsFirstTap;

    private View mTextView;
    private Callback mCallback;
    private VelocityTracker mVelocityTracker;

    private int mTouchSlop;
    private int mMinmumVelocity;
    private int mMaxmumVelocity;


    TextGestureDetector(View textView, Callback callback)
    {
        if (textView == null || callback == null)
            throw new RuntimeException("TextGestureDetector-Constructor(): null params.");

        mIsFirstTap = true;
        mIsLongTouchable = true;

        mTextView = textView;
        mCallback = callback;

        ViewConfiguration actionConfiguration
                = ViewConfiguration.get(textView.getContext());

        mTouchSlop = actionConfiguration.getScaledTouchSlop();
        mMinmumVelocity = actionConfiguration.getScaledMinimumFlingVelocity();
        mMaxmumVelocity = actionConfiguration.getScaledMaximumFlingVelocity();
    }


    public boolean isLongPressed()
    {
        return mIsLongPressed;
    }

    public void setPrevX(float prevX)
    {
        mPrevX = prevX;
    }

    public void setPrevY(float prevY)
    {
        mPrevY = prevY;
    }

    public float getPrevX()
    {
        return mPrevX;
    }

    public float getPrevY()
    {
        return mPrevY;
    }


    private void prepareVelocityTracker(MotionEvent e)
    {
        if (mVelocityTracker == null)
            mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(e);
    }

    private void releaseVelocityTracker()
    {
        if (mVelocityTracker != null)
        {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }


    boolean onTouchEvent(MotionEvent e)
    {
        boolean result = false;
        prepareVelocityTracker(e);
        mCallback.onStartEvent(e);

        switch (e.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
            {
                mIsTouchMoved = false;
                mIsLongPressed = false;

                mStartX = mPrevX = e.getX();
                mStartY = mPrevY = e.getY();
                mActivePointerId = e.getPointerId(0);

                if (mIsLongTouchable)
                    mTextView.postDelayed(
                            mLongPressRunnable,
                            ViewConfiguration.getLongPressTimeout());

                result = mCallback.onDown(e);
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = e.getActionIndex();
                mStartX = mPrevX = e.getX(index);
                mStartY = mPrevY = e.getY(index);
                mActivePointerId = e.getPointerId(index);
                break;

            case MotionEvent.ACTION_MOVE:
            {
                final int pointerIndex =
                        e.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1)
                    return false;

                float curX = e.getX(pointerIndex);
                float curY = e.getY(pointerIndex);

                float deltaX = curX - mPrevX;
                float deltaY = curY - mPrevY;

                mPrevX = curX;
                mPrevY = curY;

                //长按后拖动。
                if (mLongTappingResult && mIsLongPressed)
                {
                    mCallback.onLongTapping(false, curX, curY);
                    return true;
                }

                if (!mIsTouchMoved)
                {
                    float xDiff = Math.abs(curX - mStartX);
                    float yDiff = Math.abs(curY - mStartY);

                    if (mCallback.onDetermineCanScroll(xDiff, yDiff, mTouchSlop))
                    {
                        mIsTouchMoved = true;
                        mCallback.onStartScroll(e);
                    }
                }

                if (mIsTouchMoved)
                {
                    mTextView.removeCallbacks(mLongPressRunnable);
                    mCallback.onScroll(e, mStartX, mStartY, curX, curY, deltaX, deltaY);
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                final int pointerIndex = e.getActionIndex();
                final int pointerId = e.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId)
                {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mPrevX = e.getX(newPointerIndex);
                    mPrevY = e.getY(newPointerIndex);
                    mActivePointerId = e.getPointerId(newPointerIndex);

                    if (mVelocityTracker != null)
                        mVelocityTracker.clear();
                }
                break;

            case MotionEvent.ACTION_UP:
            {
                mCallback.onStartUp(e, mVelocityTracker);

                if (!mIsTouchMoved && !mIsLongPressed)
                {
                    mCallback.onSingleTap(e);

                    if (mIsFirstTap)
                    {
                        mIsFirstTap = false;
                        mTextView.postDelayed(mDoubleTapRunnable,
                                ViewConfiguration.getDoubleTapTimeout());
                    }
                    else
                    {
                        mCallback.onDoubleTap(e);
                        mTextView.removeCallbacks(mDoubleTapRunnable);
                        mIsFirstTap = true;
                    }
                }
                else if (mIsLongPressed && mLongTappingResult)
                    mCallback.onLongTap(e);
                else
                {
                    Log.d("onFling", "computeCurrentVelocity()");

                    mVelocityTracker.computeCurrentVelocity(1000, mMaxmumVelocity);
                    int velocityY = (int) mVelocityTracker.getYVelocity(mActivePointerId);

                    if (Math.abs(velocityY) > mMinmumVelocity)
                        mCallback.onFling(0, -velocityY);
                }

                mIsTouchMoved = false;
                mIsLongPressed = false;
                mActivePointerId = -1;

                mTextView.removeCallbacks(mLongPressRunnable);
                releaseVelocityTracker();

                result = mCallback.onUp(e);
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            {
                mIsTouchMoved = false;
                mIsLongPressed = false;
                mActivePointerId = -1;

                mTextView.removeCallbacks(mLongPressRunnable);
                mTextView.removeCallbacks(mDoubleTapRunnable);
                releaseVelocityTracker();

                mCallback.onCancel(e);
                result = true;
                break;
            }
        }

        mCallback.onEndEvent(e, mVelocityTracker, result);
        return true;
    }


    private Runnable mLongPressRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            mIsLongPressed = true;
            mLongTappingResult = mCallback.onLongTapping(true, mPrevX, mPrevY);
        }
    };

    private Runnable mDoubleTapRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            mIsFirstTap = true;
        }
    };


    interface Callback
    {
        void onStartEvent(MotionEvent e);

        void onEndEvent(MotionEvent e, VelocityTracker velocityTracker, boolean result);

        boolean onDown(MotionEvent e);

        void onStartUp(MotionEvent e, VelocityTracker velocityTracker);

        boolean onUp(MotionEvent e);

        void onCancel(MotionEvent e);

        boolean onDetermineCanScroll(float xDiff, float yDiff, int touchSlop);

        boolean onSingleTap(MotionEvent e);

        void onDoubleTap(MotionEvent e);

        boolean onLongTapping(boolean first, float prevTouchedX, float prevTouchedY);

        void onLongTap(MotionEvent e);

        void onStartScroll(MotionEvent e);

        void onScroll(MotionEvent e, float startX, float startY, float curX, float curY, float dx, float dy);

        void onFling(float velocityX, float velocityY);
    }
}
