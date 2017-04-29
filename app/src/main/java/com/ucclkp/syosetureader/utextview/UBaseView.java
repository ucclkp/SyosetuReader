package com.ucclkp.syosetureader.utextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.EdgeEffect;
import android.widget.OverScroller;


public abstract class UBaseView extends View implements NestedScrollingChild
{
    private boolean mIsTouching;
    private boolean mIsTouchMoved;
    private boolean mIsLongTouched;
    private boolean mLongClickingResult;

    private boolean mIsScrollable;
    private boolean mIsLongTouchable;
    private boolean mIsScrolling;

    private float mPrevTouchedX;
    private float mPrevTouchedY;
    private float mStartTouchedX;
    private float mStartTouchedY;

    private int mActivePointerId;

    private int mTouchSlop;
    private int mMinmumVelocity;
    private int mMaxmumVelocity;

    protected OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private NestedScrollingChildHelper mNSChildHelper;

    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;

    final String mUUID = "";


    public UBaseView(Context context)
    {
        this(context, null);
    }

    public UBaseView(Context context, AttributeSet attr)
    {
        this(context, attr, 0);
    }

    public UBaseView(Context context, AttributeSet attr, int defStyleAttr)
    {
        this(context, attr, defStyleAttr, 0);
    }

    public UBaseView(Context context, AttributeSet attr, int defStyleAttr, int defStyleRes)
    {
        super(context, attr, defStyleAttr, defStyleRes);
        initBaseView();
    }


    private void initBaseView()
    {
        ViewConfiguration actionConfiguration = ViewConfiguration.get(getContext());

        mTouchSlop = actionConfiguration.getScaledTouchSlop();
        mMinmumVelocity = actionConfiguration.getScaledMinimumFlingVelocity();
        mMaxmumVelocity = actionConfiguration.getScaledMaximumFlingVelocity();

        mIsTouching = false;
        mIsTouchMoved = false;
        mIsLongTouched = false;
        mLongClickingResult = false;
        mIsScrolling = false;

        mIsScrollable = true;
        mIsLongTouchable = true;

        mVelocityTracker = null;
        mScroller = new OverScroller(getContext());
        mNSChildHelper = new NestedScrollingChildHelper(this);

        setWillNotDraw(false);
        setClickable(true);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setScrollContainer(true);
        setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);
        setNestedScrollingEnabled(true);

        float scrollBarSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2,
                getContext().getResources().getDisplayMetrics());
        setScrollBarSize((int) scrollBarSize);
    }


    private void prepareVelocityTracker(MotionEvent e)
    {
        if (mVelocityTracker == null)
        {
            mVelocityTracker = VelocityTracker.obtain();
        }
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


    protected void drawVerticalEdgeEffect(Canvas canvas)
    {
        if (mEdgeGlowTop != null)
        {
            final int scrollY = getScrollY();
            if (!mEdgeGlowTop.isFinished())
            {
                final int restoreCount = canvas.save();
                canvas.translate(0, 0);

                mEdgeGlowTop.setSize(getWidth(), getHeight());
                if (mEdgeGlowTop.draw(canvas))
                    postInvalidateOnAnimation();

                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished())
            {
                final int restoreCount = canvas.save();
                final int width = getWidth();
                final int height = getHeight();

                canvas.translate(-width, height + scrollY);
                canvas.rotate(180, width, 0);

                mEdgeGlowBottom.setSize(width, height);
                if (mEdgeGlowBottom.draw(canvas))
                    postInvalidateOnAnimation();

                canvas.restoreToCount(restoreCount);
            }
        }
    }

    private int processVerticalMove(float deltaY)
    {
        int realDeltaY = 0;
        int displayHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int textManagerHeight = this.onRequestTextHeight();

        if (textManagerHeight > displayHeight)
        {
            if (deltaY > 0)
            {
                if (getScrollY() == 0)
                    realDeltaY = 0;
                else if (getScrollY() > 0)
                {
                    if (getScrollY() >= deltaY)
                        realDeltaY = (int) deltaY;
                    else
                        realDeltaY = getScrollY();
                }
            } else if (deltaY < 0)
            {
                if (textManagerHeight - getScrollY() == displayHeight)
                    realDeltaY = 0;
                else
                {
                    if (displayHeight - (textManagerHeight - getScrollY()) <= deltaY)
                        realDeltaY = (int) deltaY;
                    else
                        realDeltaY = (displayHeight - (textManagerHeight - getScrollY()));
                }
            }
        }

        if (realDeltaY != 0)
            scrollBy(0, -realDeltaY);

        return -realDeltaY;
    }

    public int getVerticalScrollRange()
    {
        return this.onRequestTextHeight() + getPaddingTop() + getPaddingBottom();
    }


    @Override
    public void computeScroll()
    {
        super.computeScroll();

        if (mScroller.computeScrollOffset())
        {
            int prevX = getScrollX();
            int prevY = getScrollY();
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();

            if (prevX != currX || prevY != currY)
            {
                final int range = getVerticalScrollRange();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > getHeight());

                if (canOverscroll && mEdgeGlowTop != null)
                {
                    if (currY <= 0 && prevY >= 0)
                    {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (currY + getHeight() >= range && prevY + getHeight() <= range)
                    {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }

            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    public void setOverScrollMode(int mode)
    {
        if (mode != OVER_SCROLL_NEVER)
        {
            if (mEdgeGlowTop == null)
            {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);

                TypedValue typedValue = new TypedValue();
                TypedArray a = getContext().obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.colorEdgeEffect});
                int edgeEffectColor = a.getColor(0, Color.argb(125, 125, 125, 125));
                a.recycle();

                mEdgeGlowTop.setColor(edgeEffectColor);
                mEdgeGlowBottom.setColor(edgeEffectColor);
            }
        } else
        {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
        }
        super.setOverScrollMode(mode);
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(mScrollChangedListener);
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        getViewTreeObserver().removeOnScrollChangedListener(mScrollChangedListener);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        if (!mScroller.isFinished())
            mScroller.forceFinished(true);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        drawTextLayout(canvas);
        canvas.restore();

        drawVerticalEdgeEffect(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        boolean result = super.onTouchEvent(e);

        prepareVelocityTracker(e);

        switch (e.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
            {
                if (!mScroller.isFinished())
                    mScroller.abortAnimation();

                mIsTouching = true;
                mIsTouchMoved = false;
                mIsLongTouched = false;

                mActivePointerId = e.getPointerId(0);
                mStartTouchedX = mPrevTouchedX = e.getX();
                mStartTouchedY = mPrevTouchedY = e.getY();

                if (mIsLongTouchable)
                    postDelayed(mLongPressRunnable, ViewConfiguration.getLongPressTimeout());
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = e.getActionIndex();
                mStartTouchedX = mPrevTouchedX = e.getX(index);
                mStartTouchedY = mPrevTouchedY = e.getY(index);
                mActivePointerId = e.getPointerId(index);
                return true;

            case MotionEvent.ACTION_MOVE:
            {
                final int pointerIndex =
                        e.findPointerIndex(mActivePointerId);
                if (pointerIndex == -1)
                    return false;

                float curX = e.getX(pointerIndex);
                float curY = e.getY(pointerIndex);
                float deltaY = curY - mPrevTouchedY;

                //长按后拖动。
                if (mLongClickingResult && mIsLongTouched)
                {
                    this.onLongTapping(false, curX, curY);
                    return true;
                }

                if (!mIsTouchMoved)
                {
                    float xDiff = Math.abs(curX - mStartTouchedX);
                    float yDiff = Math.abs(curY - mStartTouchedY);

                    if (xDiff > mTouchSlop || yDiff > mTouchSlop)
                    {
                        mIsTouchMoved = true;
                        mNSChildHelper.startNestedScroll(SCROLL_AXIS_VERTICAL);
                    }
                }

                if (mIsTouchMoved)
                {
                    removeCallbacks(mLongPressRunnable);

                    if (mIsScrollable)
                    {
                        processVerticalMove(deltaY);

                        final int range = getVerticalScrollRange();
                        final int overscrollMode = getOverScrollMode();
                        final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                                (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > getHeight());

                        if (canOverscroll)
                        {
                            if (-deltaY + getScrollY() < 0)
                            {
                                mEdgeGlowTop.onPull(deltaY / getHeight(), 0.5f + (curX - mStartTouchedX) / (getWidth() * 2));
                                if (!mEdgeGlowBottom.isFinished())
                                    mEdgeGlowBottom.onRelease();
                            } else if (-deltaY + getScrollY() + getHeight() > range)
                            {
                                mEdgeGlowBottom.onPull(deltaY / getHeight(), 0.5f - (curX - mStartTouchedX) / (getWidth() * 2));
                                if (!mEdgeGlowTop.isFinished())
                                    mEdgeGlowTop.onRelease();
                            }
                        }
                        if (mEdgeGlowTop != null
                                && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()))
                            postInvalidateOnAnimation();
                    }
                }

                mPrevTouchedX = curX;
                mPrevTouchedY = curY;

                return true;
            }

            case MotionEvent.ACTION_POINTER_UP:
                final int pointerIndex = e.getActionIndex();
                final int pointerId = e.getPointerId(pointerIndex);
                if (pointerId == mActivePointerId)
                {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    mPrevTouchedX = e.getX(newPointerIndex);
                    mPrevTouchedY = e.getY(newPointerIndex);
                    mActivePointerId = e.getPointerId(newPointerIndex);

                    if (mVelocityTracker != null)
                        mVelocityTracker.clear();
                }
                return true;

            case MotionEvent.ACTION_UP:
            {
                if (!mIsTouchMoved && !mIsLongTouched)
                    this.onTap(mPrevTouchedX, mPrevTouchedY);
                else if (mIsLongTouched && mLongClickingResult)
                    this.onLongTapped();
                else
                {
                    int maxY = getVerticalScrollRange() - getHeight();
                    if (maxY > 0)
                    {
                        mVelocityTracker.computeCurrentVelocity(1000);
                        int velocityX = (int) mVelocityTracker.getXVelocity();
                        int velocityY = (int) mVelocityTracker.getYVelocity();

                        if (mMinmumVelocity > Math.abs(velocityX))
                            velocityX = 0;
                        else if (mMaxmumVelocity < Math.abs(velocityX))
                            velocityX = mMaxmumVelocity * (int) Math.signum(velocityX);

                        if (mMinmumVelocity > Math.abs(velocityY))
                            velocityY = 0;
                        else if (mMaxmumVelocity < Math.abs(velocityY))
                            velocityY = mMaxmumVelocity * (int) Math.signum(velocityY);

                        if (velocityX != 0 || velocityY != 0)
                        {
                            mScroller.fling(getScrollX(), getScrollY(),
                                    -velocityX, -velocityY,
                                    0, 0,
                                    0, maxY);

                            invalidate();
                        }
                    }
                }

                mIsTouching = false;
                mIsTouchMoved = false;
                mIsLongTouched = false;
                mActivePointerId = -1;

                if (mEdgeGlowTop != null)
                {
                    mEdgeGlowTop.onRelease();
                    mEdgeGlowBottom.onRelease();
                }

                mNSChildHelper.stopNestedScroll();

                removeCallbacks(mLongPressRunnable);
                releaseVelocityTracker();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
            {
                if (mEdgeGlowTop != null)
                {
                    mEdgeGlowTop.onRelease();
                    mEdgeGlowBottom.onRelease();
                }

                mNSChildHelper.stopNestedScroll();

                mIsTouchMoved = false;
                mIsLongTouched = false;
                mActivePointerId = -1;

                removeCallbacks(mLongPressRunnable);
                releaseVelocityTracker();
                return true;
            }
        }

        return result;
    }


    public void setIsLongTouchable(boolean longTouchable)
    {
        mIsLongTouchable = longTouchable;
        if (!longTouchable)
            removeCallbacks(mLongPressRunnable);
    }

    public void setIsScrollable(boolean scrollable)
    {
        mIsScrollable = scrollable;
        if (!scrollable)
            mScroller.forceFinished(true);
    }

    public boolean isScrolling()
    {
        return mIsScrolling;
    }


    private Runnable mLongPressRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            mIsLongTouched = true;
            mLongClickingResult = UBaseView.this.onLongTapping(true, mPrevTouchedX, mPrevTouchedY);
        }
    };


    private Runnable mScrollMonitorRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (mIsTouching)
            {
                removeCallbacks(mScrollMonitorRunnable);
                postDelayed(mScrollMonitorRunnable, 100);
            } else
            {
                UBaseView.this.onEndScroll();
                mIsScrolling = false;
            }
        }
    };


    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener
            = new ViewTreeObserver.OnScrollChangedListener()
    {
        @Override
        public void onScrollChanged()
        {
            if (!mIsScrolling)
            {
                UBaseView.this.onStartScroll();
                mIsScrolling = true;
            }

            removeCallbacks(mScrollMonitorRunnable);
            postDelayed(mScrollMonitorRunnable, 100);

            UBaseView.this.onScrolling();
        }
    };


    public abstract void onTap(float prevTouchedX, float prevTouchedY);

    public abstract boolean onLongTapping(boolean first, float prevTouchedX, float prevTouchedY);

    public abstract void onLongTapped();

    public abstract int onRequestTextHeight();

    public abstract void drawTextLayout(Canvas canvas);

    public abstract void onStartScroll();

    public abstract void onScrolling();

    public abstract void onEndScroll();
}
