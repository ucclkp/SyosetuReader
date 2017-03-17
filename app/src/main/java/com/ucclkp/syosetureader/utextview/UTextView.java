package com.ucclkp.syosetureader.utextview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.text.DynamicLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;

public class UTextView extends UBaseView
{
    private UTextManager mTextManager;


    public UTextView(Context context)
    {
        this(context, null);
    }

    public UTextView(Context context, AttributeSet attr)
    {
        this(context, attr, 0);
    }

    public UTextView(Context context, AttributeSet attr, int defStyleAttr)
    {
        this(context, attr, defStyleAttr, 0);
    }

    public UTextView(Context context, AttributeSet attr, int defStyleAttr, int defStyleRes)
    {
        super(context, attr, defStyleAttr, defStyleRes);
        initTextView();
    }


    private void initTextView()
    {
        mTextManager = new UTextManager(this);
    }


    private int computeBeyondOffset()
    {
        int finalOffset = 0;
        DynamicLayout layout = mTextManager.getLayout();
        UAssistantManager assistMgr = mTextManager.getAssistantManager();

        boolean isDraggingLeftHandle = assistMgr.isDragging(UAssistantManager.WindowType.LEFT_HANDLE);
        boolean isDraggingRightHandle = assistMgr.isDragging(UAssistantManager.WindowType.RIGHT_HANDLE);
        boolean isDraggingHandle = isDraggingLeftHandle || isDraggingRightHandle;

        int startLine = layout.getLineForOffset(mTextManager.getSelectionStart());
        int endLine = layout.getLineForOffset(mTextManager.getSelectionEnd());

        int startLineTop = layout.getLineTop(startLine);
        int endLineBottom = layout.getLineBottom(endLine);

        //当选择的区域高度大于该View高度。
        if (endLineBottom - startLineTop >= getHeight())
        {
            //直接移动至选择区域底部。
            finalOffset = endLineBottom + getPaddingTop() - getHeight() - getScrollY();

            //在以上条件下，当选择区域只有一行时，即意味着此时的View高度连一行文本都放不下，
            //这时最终的移动偏移即为finalOffset，否则将继续加一些偏移，留出空间用以显示Handle。
            if (startLine != endLine)
            {
                int handleHeight = mTextManager.getAssistantManager()
                        .getHandleView(UAssistantManager.WindowType.RIGHT_HANDLE).getWindow().getHeight();

                //以下表达式中的getPaddingBottom()用于当选择区域包括最后一行时留出空间显示Handle。
                finalOffset += Math.min(handleHeight, mTextManager.getHeight() - endLineBottom + getPaddingBottom());
            }
        }
        else
        {
            int prevHeight;
            int nextHeight;

            if (startLine == 0)
                prevHeight = getPaddingTop();
            else if (startLine == 1)
                prevHeight = startLineTop - layout.getLineTop(startLine - 1) + getPaddingTop();
            else
                prevHeight = startLineTop - layout.getLineTop(startLine - 2);

            if (layout.getLineCount() - 1 - endLine == 0)
                nextHeight = getPaddingBottom();
            else if (layout.getLineCount() - 1 - endLine == 1)
                nextHeight = layout.getLineBottom(endLine + 1) - endLineBottom + getPaddingBottom();
            else
                nextHeight = layout.getLineBottom(endLine + 2) - endLineBottom;


            if (prevHeight + nextHeight + endLineBottom - startLineTop >= getHeight())
                finalOffset = Math.max(endLineBottom + getPaddingTop() - getHeight() - getScrollY(), 0 - getScrollY());
            else if (startLineTop + getPaddingTop() - getScrollY() < prevHeight)
                finalOffset = startLineTop + getPaddingTop() - prevHeight - getScrollY();
            else if (endLineBottom + getPaddingTop() - getScrollY() + nextHeight > getHeight())
                finalOffset = endLineBottom + getPaddingTop() + nextHeight - getHeight() - getScrollY();
        }

        return finalOffset;
    }

    private int computeOverScrollOffset()
    {
        int offsetY = 0;
        int visHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        if (visHeight + getScrollY() > mTextManager.getHeight())
        {
            if (visHeight >= mTextManager.getHeight())
                offsetY = 0 - getScrollY();
            else
                offsetY = mTextManager.getHeight() - visHeight - getScrollY();
        }

        return offsetY;
    }


    public boolean scrollToFit()
    {
        if (mTextManager == null)
            return false;

        int beyondOffsetY = 0;
        int overScrollOffsetY;

        if (mTextManager.hasSelection())
            beyondOffsetY = computeBeyondOffset();

        overScrollOffsetY = computeOverScrollOffset();

        int offsetY = overScrollOffsetY != 0 ? overScrollOffsetY : beyondOffsetY;

        if (offsetY != 0)
        {
            mScroller.startScroll(getScrollX(), getScrollY(), 0, offsetY);
            invalidate();
            return true;
        }

        return false;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        int finalWidth;
        int finalHeight;

        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int horizontalPadding = getPaddingLeft() + getPaddingRight();

        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        switch (widthMode)
        {
            case View.MeasureSpec.AT_MOST:
                mTextManager.setMaxWidth(width - horizontalPadding);
                finalWidth = Math.max(
                        getSuggestedMinimumWidth(),
                        mTextManager.getWidth() + horizontalPadding);
                finalWidth = Math.min(width, finalWidth);
                break;

            case View.MeasureSpec.EXACTLY:
                finalWidth = width;
                mTextManager.setMaxWidth(finalWidth - horizontalPadding);
                break;

            case View.MeasureSpec.UNSPECIFIED:
            default:
                finalWidth = 0;
                break;
        }

        switch (heightMode)
        {
            case View.MeasureSpec.AT_MOST:
                finalHeight = Math.max(
                        getSuggestedMinimumHeight(),
                        mTextManager.getHeight() + verticalPadding);
                finalHeight = Math.min(height, finalHeight);
                break;

            case View.MeasureSpec.EXACTLY:
                finalHeight = height;
                break;

            case View.MeasureSpec.UNSPECIFIED:
            default:
                finalHeight = Math.max(
                        getSuggestedMinimumHeight(),
                        mTextManager.getHeight() + verticalPadding);
                break;
        }

        setMeasuredDimension(finalWidth, finalHeight);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight)
    {
        super.onSizeChanged(width, height, oldWidth, oldHeight);

        mTextManager.onSizeChanged();

        if (scrollToFit())
            return;

        if (!mTextManager.hasSelection() && getVerticalScrollRange() > getHeight())
        {
            int prevTextOffset = mTextManager.getTextOffsetAtViewTop();
            mTextManager.computeTextOffsetAtViewTop();
            int curTextOffset = mTextManager.getTextOffsetAtViewTop();
            if (curTextOffset != prevTextOffset)
            {
                mTextManager.setTextOffsetAtViewTop(prevTextOffset);
                int scrollOffsetY = mTextManager.getScrollOffsetFromTextOffset(prevTextOffset);
                if (scrollOffsetY != 0)
                    scrollBy(0, scrollOffsetY);
            }
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect)
    {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        Log.d("PricipleConfirmer",
                getClass().getName() + "(" + mUUID + ")"
                        + " onFocusChanged(gainFocus: " + gainFocus + ")");

        mTextManager.onFocusChanged(gainFocus);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus)
    {
        super.onWindowFocusChanged(hasWindowFocus);

        Log.d("PricipleConfirmer",
                getClass().getName() + "(" + mUUID + ")"
                        + " onWindowFocusChanged(gainFocus: " + hasWindowFocus + ")");

        mTextManager.onWindowFocusChanged(hasWindowFocus);
    }

    @Override
    public void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        mTextManager.onDetachedFromWindow();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        return mTextManager.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        return mTextManager.onKeyPreIme(keyCode, event) || super.onKeyPreIme(keyCode, event);
    }


    @Override
    public void onTap(float prevTouchedX, float prevTouchedY)
    {
        mTextManager.onClicked(prevTouchedX, prevTouchedY);
    }

    @Override
    public boolean onLongTapping(boolean first, float prevTouchedX, float prevTouchedY)
    {
        return mTextManager.onLongClicking(first, prevTouchedX, prevTouchedY);
    }

    @Override
    public void onLongTapped()
    {
        mTextManager.onLongClicked();
    }

    @Override
    public int onRequestTextHeight()
    {
        return mTextManager.getHeight();
    }

    @Override
    public void drawTextLayout(Canvas canvas)
    {
        mTextManager.onDraw(canvas);
    }

    @Override
    public void onStartScroll()
    {
        mTextManager.onStartScroll();
    }

    @Override
    public void onScrolling()
    {
        mTextManager.onScrolling();
    }

    @Override
    public void onEndScroll()
    {
        mTextManager.onEndScroll();
    }


    public void setText(CharSequence text)
    {
        mTextManager.setText(text);
    }

    public void setActionModeStyle(int style)
    {
        mTextManager.setActionModeStyle(style);
    }

    public void setTextOffsetAtScreenTop(int offset)
    {
        mTextManager.setTextOffsetAtViewTop(offset);
    }


    public CharSequence getText()
    {
        return mTextManager.getText();
    }

    public DynamicLayout getLayout()
    {
        return mTextManager.getLayout();
    }

    public int getTextOffsetAtScreenTop()
    {
        return mTextManager.getTextOffsetAtViewTop();
    }


    public void scrollToTextOffset(int offset, boolean smoothScroll)
    {
        int yOffset = mTextManager.getScrollOffsetFromTextOffset(offset);
        if (yOffset != 0)
        {
            if (smoothScroll)
            {
                mScroller.startScroll(getScrollX(), getScrollY(), 0, yOffset);
                invalidate();
            }
            else
                scrollBy(0, yOffset);
        }
    }
}
