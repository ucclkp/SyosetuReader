package com.ucclkp.syosetureader.utextview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.ucclkp.syosetureader.R;

public class UAssistantManager
{
    private View mTargetView;
    private UTextManager mTextManager;

    private HandleView mLeftHandleView;
    private HandleView mRightHandleView;

    private HandleEventListener mHandleEventListener;

    private Path mSelectionPath;
    private Paint mSelectionPathPaint;

    private Rect mRedrawBounds;
    private RectF mSelectionBounds;

    private final int[] mViewLocationOnScreen = new int[2];
    private final int[] mWindowLocationOnScreen = new int[2];


    public enum AssistType
    {
        SELECTION,
        LEFT_HANDLE,
        RIGHT_HANDLE,
    }

    public enum WindowType
    {
        LEFT_HANDLE,
        RIGHT_HANDLE,
    }


    public UAssistantManager(UTextManager textManager)
    {
        mTextManager = textManager;
        mTargetView = textManager.getView();
        Context context = textManager.getView().getContext();

        mLeftHandleView = new HandleView(context,
                R.drawable.text_select_handle_left,
                WindowType.LEFT_HANDLE);

        mRightHandleView = new HandleView(context,
                R.drawable.text_select_handle_right,
                WindowType.RIGHT_HANDLE);

        mRedrawBounds = new Rect();
        mSelectionBounds = new RectF();

        mSelectionPath = new Path();
        mSelectionPath.reset();
        mSelectionPathPaint = new Paint();

        TypedArray a = mTargetView.getContext().obtainStyledAttributes(
                new int[]{android.R.attr.colorAccent, android.R.attr.textColorHighlight});
        int accentColor = a.getColor(a.getIndex(0), Color.BLACK);
        int highlightColor = a.getColor(a.getIndex(1), Color.parseColor("#1e88e5"));
        a.recycle();

        mSelectionPathPaint.setColor(highlightColor);
    }


    public void setHandleEventListener(HandleEventListener l)
    {
        mHandleEventListener = l;
    }


    public void closeAllHandle()
    {
        closeLeftHandle();
        closeRightHandle();
    }

    public boolean isShowing(AssistType type)
    {
        boolean showing = false;

        switch (type)
        {
            case SELECTION:
                showing = !mSelectionPath.isEmpty();
                break;

            case LEFT_HANDLE:
                showing = mLeftHandleView.isShowing();
                break;

            case RIGHT_HANDLE:
                showing = mRightHandleView.isShowing();
                break;
        }

        return showing;
    }


    public boolean showLeftHandle()
    {
        return showLeftHandle(false);
    }

    public boolean showLeftHandle(boolean forceUpdate)
    {
        if (!isLeftHandleBeyond())
        {
            calLeftHandleLocation(mWindowLocationOnScreen);

            int x = mWindowLocationOnScreen[0];
            int y = mWindowLocationOnScreen[1];
            mLeftHandleView.show(x, y, forceUpdate);

            return true;
        } else
        {
            mLeftHandleView.close();
            return false;
        }
    }

    public boolean showRightHandle()
    {
        return showRightHandle(false);
    }

    public boolean showRightHandle(boolean forceUpdate)
    {
        if (!isRightHandleBeyond())
        {
            calRightHandleLocation(mWindowLocationOnScreen);

            int x = mWindowLocationOnScreen[0];
            int y = mWindowLocationOnScreen[1];
            mRightHandleView.show(x, y, forceUpdate);

            return true;
        } else
        {
            mRightHandleView.close();
            return false;
        }
    }

    public void drawSelectionHighlight()
    {
        drawSelectionHighlight(
                mTextManager.getSelectionStart(),
                mTextManager.getSelectionEnd());
    }

    public void drawSelectionHighlight(int start, int end)
    {
        mTextManager.getLayout().getSelectionPath(start, end, mSelectionPath);

        mSelectionPath.computeBounds(mSelectionBounds, true);
        mRedrawBounds.set(
                (int) Math.floor(mSelectionBounds.left + mTargetView.getPaddingLeft()),
                (int) Math.floor(mSelectionBounds.top + mTargetView.getPaddingTop()),
                (int) Math.ceil(mSelectionBounds.right + mTargetView.getPaddingLeft()),
                (int) Math.ceil(mSelectionBounds.bottom + mTargetView.getPaddingTop()));

        mTargetView.invalidate(mRedrawBounds);
    }

    public void closeLeftHandle()
    {
        mLeftHandleView.close();
    }

    public void closeRightHandle()
    {
        mRightHandleView.close();
    }


    public void eraseSelectionHighlight()
    {
        eraseSelectionHighlight(
                mTextManager.getSelectionStart(),
                mTextManager.getSelectionEnd());
    }

    public void eraseSelectionHighlight(int start, int end)
    {
        mTextManager.getLayout().getSelectionPath(start, end, mSelectionPath);

        mSelectionPath.computeBounds(mSelectionBounds, true);
        mRedrawBounds.set(
                (int) Math.floor(mSelectionBounds.left + mTargetView.getPaddingLeft()),
                (int) Math.floor(mSelectionBounds.top + mTargetView.getPaddingTop()),
                (int) Math.ceil(mSelectionBounds.right + mTargetView.getPaddingLeft()),
                (int) Math.ceil(mSelectionBounds.bottom + mTargetView.getPaddingTop()));

        mSelectionPath.reset();
        mTargetView.invalidate(mRedrawBounds);
    }


    public boolean isLeftHandleBeyond()
    {
        int selectionStart = mTextManager.getSelectionStart();

        int offsetLine = mTextManager.getLayout().getLineForOffset(selectionStart);
        float offsetX = mTextManager.getLayout().getPrimaryHorizontal(selectionStart);
        float offsetLineBottom = mTextManager.getLayout().getLineBottom(offsetLine);

        int x = (int) (offsetX - mTargetView.getScrollX());
        int y = (int) (offsetLineBottom - mTargetView.getScrollY());

        return ((x < -mTargetView.getPaddingLeft() || x > mTargetView.getWidth() - mTargetView.getPaddingRight())
                || (y < -mTargetView.getPaddingTop() || y > mTargetView.getHeight() - mTargetView.getPaddingBottom()));
    }

    public boolean isRightHandleBeyond()
    {
        int selectionEnd = mTextManager.getSelectionEnd();

        int offsetLine = mTextManager.getLayout().getLineForOffset(selectionEnd);
        float offsetX = mTextManager.getLayout().getPrimaryHorizontal(selectionEnd);
        float offsetLineBottom = mTextManager.getLayout().getLineBottom(offsetLine);

        int x = (int) (offsetX - mTargetView.getScrollX());
        int y = (int) (offsetLineBottom - mTargetView.getScrollY());

        return ((x < -mTargetView.getPaddingLeft() || x > mTargetView.getWidth() - mTargetView.getPaddingRight())
                || (y < -mTargetView.getPaddingTop() || y > mTargetView.getHeight() - mTargetView.getPaddingBottom()));
    }


    public boolean isDragging(WindowType type)
    {
        switch (type)
        {
            case LEFT_HANDLE:
                return mLeftHandleView.isDragging();

            case RIGHT_HANDLE:
                return mRightHandleView.isDragging();
        }

        return false;
    }

    public HandleView getHandleView(WindowType type)
    {
        switch (type)
        {
            case LEFT_HANDLE:
                return mLeftHandleView;

            case RIGHT_HANDLE:
                return mRightHandleView;
        }

        return null;
    }


    public Path getSelectionHighlighPath()
    {
        return mSelectionPath;
    }

    public RectF getSelectionBounds()
    {
        return mSelectionBounds;
    }

    public Paint getSelectionHighlighPaint()
    {
        return mSelectionPathPaint;
    }


    public void computeContentRect(RectF outRect)
    {
        if (mTextManager.hasSelection())
        {
            outRect.set(mSelectionBounds);
            if (isShowing(AssistType.RIGHT_HANDLE))
                outRect.bottom += mRightHandleView.getWindow().getHeight();
        }
    }


    private void calLeftHandleLocation(int[] outLocation)
    {
        int selectionStart = mTextManager.getSelectionStart();

        mTargetView.getLocationOnScreen(mViewLocationOnScreen);

        int startOffsetLine = mTextManager.getLayout().getLineForOffset(selectionStart);
        float startOffsetX = mTextManager.getLayout().getPrimaryHorizontal(selectionStart);
        float startOffsetLineBottom = mTextManager.getLayout().getLineBottom(startOffsetLine);

        int x = (int) (mViewLocationOnScreen[0] + mTargetView.getPaddingLeft()
                + startOffsetX - mTargetView.getScrollX()
                - mLeftHandleView.getWindow().getWidth() * 3 / 4f);

        int y = (int) (mViewLocationOnScreen[1] + mTargetView.getPaddingTop()
                - mTargetView.getScrollY()
                + startOffsetLineBottom);

        outLocation[0] = x;
        outLocation[1] = y;
    }

    private void calRightHandleLocation(int[] outLocation)
    {
        int selectionEnd = mTextManager.getSelectionEnd();

        mTargetView.getLocationOnScreen(mViewLocationOnScreen);

        int endOffsetLine = mTextManager.getLayout().getLineForOffset(selectionEnd);
        float endOffsetX = mTextManager.getLayout().getPrimaryHorizontal(selectionEnd);
        float endOffsetLineBottom = mTextManager.getLayout().getLineBottom(endOffsetLine);

        int x = (int) (mViewLocationOnScreen[0] + mTargetView.getPaddingLeft()
                + endOffsetX - mTargetView.getScrollX()
                - mRightHandleView.getWindow().getWidth() / 4f);

        int y = (int) (mViewLocationOnScreen[1] + mTargetView.getPaddingTop()
                - mTargetView.getScrollY()
                + endOffsetLineBottom);

        outLocation[0] = x;
        outLocation[1] = y;
    }


    public class HandleView extends View
    {
        private int mWindowX, mWindowY;

        protected Drawable mDrawable;
        protected WindowType mWindowType;
        private PopupWindow mContainerWindow;

        private float mStartTouchedX;
        private float mStartTouchedY;
        private boolean mIsTouchDown;
        private boolean mIsTouchMoved;


        public HandleView(Context context, int backgroundRes, WindowType type)
        {
            super(context);

            mWindowX = -1;
            mWindowY = -1;
            mWindowType = type;

            mIsTouchDown = false;
            mIsTouchMoved = false;

            mDrawable = context.getDrawable(backgroundRes);
            if (mDrawable != null)
            {
                mDrawable.setBounds(0, 0,
                        mDrawable.getIntrinsicWidth(),
                        mDrawable.getIntrinsicHeight());
            }

            mContainerWindow = new PopupWindow(context);
            mContainerWindow.setSplitTouchEnabled(true);
            mContainerWindow.setClippingEnabled(false);
            mContainerWindow.setBackgroundDrawable(null);
            mContainerWindow.setWidth(mDrawable.getIntrinsicWidth());
            mContainerWindow.setHeight(mDrawable.getIntrinsicHeight());
            mContainerWindow.setAnimationStyle(R.style.TextAssistWindowAnimation);
            mContainerWindow.setContentView(this);

            setClickable(true);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
        {
            setMeasuredDimension(
                    mDrawable.getIntrinsicWidth(),
                    mDrawable.getIntrinsicHeight());
        }

        @Override
        public void onDraw(Canvas canvas)
        {
            if (mDrawable != null)
                mDrawable.draw(canvas);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e)
        {
            switch (e.getActionMasked())
            {
                case MotionEvent.ACTION_DOWN:
                    mIsTouchDown = true;
                    mIsTouchMoved = false;
                    mStartTouchedX = e.getRawX();
                    mStartTouchedY = e.getRawY();
                    if (mHandleEventListener != null)
                        mHandleEventListener.onCapture(this, e);
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mIsTouchDown)
                    {
                        float curRawX = e.getRawX();
                        float curRawY = e.getRawY();

                        float slope = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP, 4,
                                mTargetView.getContext().getResources().getDisplayMetrics());

                        boolean canTouchMove = mIsTouchMoved ||
                                (Math.abs(curRawX - mStartTouchedX) > slope
                                        || Math.abs(curRawY - mStartTouchedY) > slope);

                        if (canTouchMove)
                        {
                            mIsTouchMoved = true;
                            if (mHandleEventListener != null)
                                mHandleEventListener.onDrag(this, e);
                        }
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_UP:
                    if (!mIsTouchMoved)
                    {
                        if (mHandleEventListener != null)
                            mHandleEventListener.onClick(this, e);
                    }

                    if (mHandleEventListener != null)
                        mHandleEventListener.onRelease(this);

                    mIsTouchDown = false;
                    mIsTouchMoved = false;
                    break;
            }
            return super.onTouchEvent(e);
        }


        public PopupWindow getWindow()
        {
            return mContainerWindow;
        }

        public int getWindowX()
        {
            return mWindowX;
        }

        public int getWindowY()
        {
            return mWindowY;
        }

        public WindowType getWindowType()
        {
            return mWindowType;
        }

        public void show(int x, int y)
        {
            show(x, y, false);
        }

        //forceUpdate: 为true时，将立即更新窗口位置。
        public void show(int x, int y, boolean forceUpdate)
        {
            mWindowX = x;
            mWindowY = y;

            if (isDragging() || forceUpdate)
            {
                if (mContainerWindow.isShowing())
                    mContainerWindow.update(x, y, -1, -1);
                else
                    mContainerWindow.showAtLocation(mTextManager.getView(), Gravity.NO_GRAVITY, x, y);
            } else
            {
                if (mContainerWindow.isShowing())
                    mContainerWindow.dismiss();

                mContainerWindow.showAtLocation(mTextManager.getView(), Gravity.NO_GRAVITY, x, y);
            }
        }

        public void close()
        {
            mContainerWindow.dismiss();
        }

        public boolean isShowing()
        {
            return mContainerWindow.isShowing();
        }

        public boolean isDragging()
        {
            return mIsTouchMoved;
        }
    }


    public interface HandleEventListener
    {
        void onCapture(HandleView view, MotionEvent e);

        void onDrag(HandleView view, MotionEvent e);

        void onClick(HandleView view, MotionEvent e);

        void onRelease(HandleView view);
    }
}