package com.ucclkp.syosetureader.statictextview;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.preference.PreferenceManager;

import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.UApplication;

import java.text.BreakIterator;
import java.util.List;


public class StaticTextView extends View implements NestedScrollingChild {
    private int mTextColor;
    private int mTextLinkColor;
    private float mTextSize;
    private float mLineSpacingMult, mLineSpacingAdd;
    private Typeface mTypeface;
    private Layout.Alignment mTextAlignment;
    private CharSequence mText;

    private TextPaint mTextPaint;
    private StaticLayout mTextLayout;
    private TextGestureDetector mGestureDetector;
    private TextIterator mTextIterator;
    private HandleManager mHandleMgr;
    private SelectionManager mSelectionMgr;
    private ActionMode mTextActionMode;
    private ActionMode.Callback mTextActionModeCallback;
    private ClipboardManager mClipMgr;
    private TextIntentProcessor mTextIntentProcessor;

    private OnSelectionChangedListener mSelectionListener;

    private OverScroller mScroller;
    private EdgeEffect mEdgeGlowTop;
    private EdgeEffect mEdgeGlowBottom;

    private final NestedScrollingChildHelper mChildHelper
            = new NestedScrollingChildHelper(this);

    private int mTextOffsetAtViewTop;
    private int mOffsetFromTextOffset;
    private int mLongTapSelectionInitEnd;
    private int mLongTapSelectionInitStart;

    private boolean mIsScrolling;
    private boolean mIsFingerOnScreen;


    private final static int MENU_ITEM_ID_COPY = 1;
    private final static int MENU_ITEM_ID_SELECT_ALL = 4;
    private final static int MENU_ITEM_ID_WEB_SEARCH = 5;

    private final static int MENU_ITEM_ORDER_COPY = 1;
    private final static int MENU_ITEM_ORDER_SELECT_ALL = 4;
    private final static int MENU_ITEM_ORDER_WEB_SEARCH = 5;
    private final static int MENU_ITEM_ORDER_TEXT_PROCESS = 0;


    public StaticTextView(Context context) {
        this(context, null);
    }

    public StaticTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StaticTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        int textColor, textLinkColor;
        TypedArray a = context.obtainStyledAttributes(
                new int[]{android.R.attr.textColorPrimary, androidx.appcompat.R.attr.colorAccent});
        textColor = a.getColor(a.getIndex(0), Color.BLACK);
        textLinkColor = a.getColor(a.getIndex(1), Color.BLUE);
        a.recycle();

        float textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 14,
                context.getResources().getDisplayMetrics());
        float lineSpacingAdd = 0.f;
        float lineSpacingMult = 1.f;
        Typeface typeface = null;

        TypedArray attrArray = context.obtainStyledAttributes(
                attrs, R.styleable.StaticTextView);
        mTextSize = attrArray.getDimension(R.styleable.StaticTextView_textSize, textSize);
        mTextColor = attrArray.getColor(R.styleable.StaticTextView_textColor, textColor);
        mTextLinkColor = attrArray.getColor(R.styleable.StaticTextView_textLinkColor, textLinkColor);
        mLineSpacingAdd = attrArray.getFloat(R.styleable.StaticTextView_lineSpacingAdd, lineSpacingAdd);
        mLineSpacingMult = attrArray.getFloat(R.styleable.StaticTextView_lineSpacingMult, lineSpacingMult);
        attrArray.recycle();

        setScrollContainer(true);
        setNestedScrollingEnabled(true);
        setVerticalScrollBarEnabled(true);
        setScrollBarStyle(SCROLLBARS_OUTSIDE_OVERLAY);

        mText = "";
        mTextAlignment = Layout.Alignment.ALIGN_NORMAL;
        mTextOffsetAtViewTop = 0;
        mOffsetFromTextOffset = 0;

        mIsScrolling = false;
        mIsFingerOnScreen = false;

        mScroller = new OverScroller(getContext());
        mTextIterator = new TextIterator();
        mGestureDetector = new TextGestureDetector(this, mTextGDCallback);
        mHandleMgr = new HandleManager();
        mSelectionMgr = new SelectionManager();
        mTextActionModeCallback = getActionModeCallback();
        mTextIntentProcessor = new TextIntentProcessor();

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.linkColor = mTextLinkColor;
        mTextPaint.density = context.getResources().getDisplayMetrics().density;
        mTextPaint.setTextSize(mTextSize);

        if (!isInEditMode())
            mClipMgr = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }


    private void makeNewTextLayout() {
        if (mTextLayout != null) {
            int prevWidth = mTextLayout.getWidth();
            mTextLayout = new StaticLayout(
                    mText, mTextPaint, prevWidth, mTextAlignment,
                    mLineSpacingMult, mLineSpacingAdd, false);
        }
    }

    private void makeNewTextLayout(int width) {
        if (mTextLayout == null
                || mTextLayout.getWidth() != width) {
            mTextLayout = new StaticLayout(
                    mText, mTextPaint, width, mTextAlignment,
                    mLineSpacingMult, mLineSpacingAdd, false);
        }
    }


    private int processVerticalMove(int deltaY) {
        int realDeltaY = 0;
        int displayHeight = getHeight() - getPaddingTop() - getPaddingBottom();
        int textHeight = mTextLayout.getHeight();

        if (textHeight > displayHeight) {
            if (deltaY > 0) {
                if (getScrollY() > 0) {
                    realDeltaY = Math.min(getScrollY(), deltaY);
                }
            } else if (deltaY < 0) {
                if (textHeight - getScrollY() != displayHeight) {
                    realDeltaY = Math.max(displayHeight - (textHeight - getScrollY()), deltaY);
                }
            }
        }

        if (realDeltaY != 0)
            scrollBy(0, -realDeltaY);

        return realDeltaY;
    }

    private int getTextHeightWithPadding() {
        return mTextLayout.getHeight() + getPaddingTop() + getPaddingBottom();
    }

    private int getVerticalScrollRange() {
        int scrollRange = 0;
        if (mTextLayout != null) {
            scrollRange = Math.max(0,
                    mTextLayout.getHeight() - (getHeight() - getPaddingBottom() - getPaddingTop()));
        }
        return scrollRange;
    }

    private void drawVerticalEdgeEffect(Canvas canvas) {
        if (mEdgeGlowTop != null) {
            final int scrollY = getScrollY();
            if (!mEdgeGlowTop.isFinished()) {
                final int restoreCount = canvas.save();
                canvas.translate(0, 0);

                mEdgeGlowTop.setSize(getWidth(), getHeight());
                if (mEdgeGlowTop.draw(canvas))
                    postInvalidateOnAnimation();

                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeGlowBottom.isFinished()) {
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


    private float convertToTextX(float vx) {
        vx -= getPaddingLeft();
        vx = Math.max(0f, vx);
        vx = Math.min(getWidth()
                - getPaddingLeft()
                - getPaddingRight() - 1, vx);
        vx += getScrollX();
        return vx;
    }

    private float convertToTextY(float vy) {
        vy -= getPaddingTop();
        vy = Math.max(0f, vy);
        vy = Math.min(getHeight()
                - getPaddingTop()
                - getPaddingBottom() - 1, vy);
        vy += getScrollY();
        return vy;
    }


    private long packRange(int start, int end) {
        return (((long) start) << 32) | end;
    }

    private int unpackRangeStart(long range) {
        return (int) (range >>> 32);
    }

    private int unpackRangeEnd(long range) {
        return (int) (range & 0x00000000FFFFFFFFL);
    }


    private void startTextActionMode() {
        if (mTextActionMode == null) {
            mTextActionMode = startActionMode(
                    mTextActionModeCallback, ActionMode.TYPE_FLOATING);
        }
    }

    private void stopTextActionMode() {
        if (mTextActionMode != null)
            mTextActionMode.finish();
    }


    private int computeBeyondOffset() {
        int finalOffset = 0;
        StaticLayout layout = mTextLayout;

        boolean isLHDragging = mHandleMgr.mLeftHandle.isDragging();
        boolean isRHDragging = mHandleMgr.mRightHandle.isDragging();
        boolean isHandleDragging = isLHDragging || isRHDragging;

        int startLine = layout.getLineForOffset(getSelectionStart());
        int endLine = layout.getLineForOffset(getSelectionEnd());

        int startLineTop = layout.getLineTop(startLine);
        int endLineBottom = layout.getLineBottom(endLine);

        //当选择的区域高度大于该View高度。
        if (endLineBottom - startLineTop >= getHeight()) {
            //直接移动至选择区域底部。
            finalOffset = endLineBottom + getPaddingTop() - getHeight() - getScrollY();

            //在以上条件下，当选择区域只有一行时，即意味着此时的View高度连一行文本都放不下，
            //这时最终的移动偏移即为finalOffset，否则将继续加一些偏移，留出空间用以显示Handle。
            if (startLine != endLine) {
                int handleHeight = mHandleMgr.mRightHandle.getHeight();

                //以下表达式中的getPaddingBottom()用于当选择区域包括最后一行时留出空间显示Handle。
                finalOffset += Math.min(handleHeight, layout.getHeight() - endLineBottom + getPaddingBottom());
            }
        } else {
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
                finalOffset = Math.max(endLineBottom + getPaddingTop() - getHeight() - getScrollY(), -getScrollY());
            else if (startLineTop + getPaddingTop() - getScrollY() < prevHeight)
                finalOffset = startLineTop + getPaddingTop() - prevHeight - getScrollY();
            else if (endLineBottom + getPaddingTop() - getScrollY() + nextHeight > getHeight())
                finalOffset = endLineBottom + getPaddingTop() + nextHeight - getHeight() - getScrollY();
        }

        return finalOffset;
    }

    public boolean scrollToFit(boolean smooth) {
        boolean scrolled = false;

        if (mTextLayout == null) return scrolled;

        if (getTextHeightWithPadding() <= getHeight()) {
            if (getScrollY() != 0) {
                if (smooth)
                    mScroller.startScroll(
                            getScrollX(), getScrollY(),
                            0, -getScrollY());
                else
                    scrollTo(0, 0);

                scrolled = true;
            }
        } else {
            int scrollOffset = 0;

            //Handle为第一优先，尽量使其处于屏幕中央。
            if (hasSelection())
                scrollOffset = computeBeyondOffset();
            else {
                //置于大小变化前的位置。
                int prevTextOffset = getTextOffsetAtViewTop();
                int prevOffset = getOffsetFromCurTextOffset();

                computeTextOffsetAtViewTop();

                int curTextOffset = getTextOffsetAtViewTop();
                if (curTextOffset != prevTextOffset) {
                    setTextOffsetAtViewTop(prevTextOffset, prevOffset);
                    scrollOffset = getScrollOffsetFromTextOffset(prevTextOffset) + prevOffset;
                }
            }

            //检查是否超出屏幕。
            int maxScrollOffset = mTextLayout.getHeight()
                    + getPaddingTop() + getPaddingBottom()
                    - (getHeight() + getScrollY());
            int offsetY = Math.min(maxScrollOffset, scrollOffset);
            if (offsetY != 0) {
                if (smooth)
                    mScroller.startScroll(
                            getScrollX(), getScrollY(),
                            0, offsetY);
                else
                    scrollBy(0, offsetY);

                scrolled = true;
            }
        }

        return scrolled;
    }


    private void relocateSelectionPath(Path sp) {
        sp.offset(0, -mTextSize * (mLineSpacingMult - 1) / 2.f);
    }

    private void computeSelectionPath(int start, int end, Path p) {
        if (p == null) return;
        p.reset();
        if (start == end) return;

        int startLine = mTextLayout.getLineForOffset(start);
        int endLine = mTextLayout.getLineForOffset(end);
        int startLineTop = mTextLayout.getLineTop(startLine);
        int endLineBottom = mTextLayout.getLineBottom(endLine);
        float startHori = mTextLayout.getPrimaryHorizontal(start);
        float endHori = mTextLayout.getPrimaryHorizontal(end);

        if (startLine == endLine) {
            float lineHeight = ((endLineBottom - startLineTop) - mTextLayout.getSpacingAdd())
                    / mTextLayout.getSpacingMultiplier();
            p.addRect(startHori, startLineTop, endHori, startLineTop + lineHeight, Path.Direction.CW);
        } else {
            float startLineRight = mTextLayout.getLineRight(startLine);
            int startLineBottom = mTextLayout.getLineBottom(startLine);
            float endLineLeft = mTextLayout.getLineLeft(endLine);
            int endLineTop = mTextLayout.getLineTop(endLine);
            p.addRect(startHori, startLineTop, startLineRight, startLineBottom, Path.Direction.CW);
            p.addRect(endLineLeft, endLineTop, endHori, endLineBottom, Path.Direction.CW);

            for (int i = startLine + 1; i < endLine; ++i) {
                p.addRect(mTextLayout.getLineLeft(i),
                        mTextLayout.getLineTop(i),
                        mTextLayout.getLineRight(i),
                        mTextLayout.getLineBottom(i),
                        Path.Direction.CW);
            }
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getViewTreeObserver().addOnScrollChangedListener(mScrollChangedListener);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        stopTextActionMode();
        mHandleMgr.closeAllHandle();
        getViewTreeObserver().removeOnScrollChangedListener(mScrollChangedListener);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        //在view第一次显示时，onFocusChanged(true)先调用，
        //然后调用onWindowFocusChanged(true)。此时我们希望
        //只在onWindowFocusChanged()中处理对获得焦点的响应。
        if (!gainFocus && hasWindowFocus()) {
            mSelectionMgr.eraseSelectionHighlight();
            mSelectionMgr.setSelection(0);

            mHandleMgr.closeAllHandle();
            stopTextActionMode();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        //在view第一次显示时，onFocusChanged(true)先调用，
        //然后调用此方法(true)。在某些情况下，比如屏幕上显示
        //多个fragment，每个fragment中都有此view，那么即使
        //某些fragment设定为不可见(GONE)，这些fragment中的
        //view也会接受到此方法的调用。由于这些view不可见，此时
        //我们不希望此处的代码被执行，因此设立此条件。
        if (hasWindowFocus && hasFocus()) {
            //在失去焦点时，我们关闭了所有显示的Assistant(除了Selection背景)，
            //那么当再次获得焦点时，理应重新显示滑动之前显示的Assistant(除了两种情况：
            //1.Beyond 2.在失去焦点期间触发其他的代码段使得Assistant被关闭)。那么我们
            //应该在失去焦点时标记已显示的Assistant，并在此处检查标记，显示这些
            //窗口，这需要额外的变量。
            if (hasSelection()) {
                mHandleMgr.showLeftHandle();
                mHandleMgr.showRightHandle();
            }
        } else if (!hasWindowFocus && hasFocus()) {
            mHandleMgr.closeAllHandle();
        }
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if (mScroller.computeScrollOffset()) {
            int prevX = getScrollX();
            int prevY = getScrollY();
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();

            if (prevX != currX || prevY != currY) {
                final int range = getTextHeightWithPadding();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > getHeight());

                if (canOverscroll && mEdgeGlowTop != null) {
                    if (currY <= 0 && prevY >= 0) {
                        mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                    } else if (currY + getHeight() >= range && prevY + getHeight() <= range) {
                        mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                }
            }

            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        return getTextHeightWithPadding();
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return super.computeVerticalScrollOffset();
    }

    @Override
    protected int computeVerticalScrollExtent() {
        return super.computeVerticalScrollExtent();
    }

    @Override
    public void setOverScrollMode(int mode) {
        if (mode != OVER_SCROLL_NEVER) {
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
        }
        super.setOverScrollMode(mode);
    }

    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed,
                offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int finalWidth = 0;
        int finalHeight = 0;

        int verticalPadding = getPaddingTop() + getPaddingBottom();
        int horizontalPadding = getPaddingLeft() + getPaddingRight();

        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int height = View.MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = View.MeasureSpec.getMode(heightMeasureSpec);

        switch (widthMode) {
            case View.MeasureSpec.AT_MOST:
                finalWidth = Math.max(getSuggestedMinimumWidth(),
                        (int) Math.ceil(Layout.getDesiredWidth(mText, mTextPaint)) + horizontalPadding);
                finalWidth = Math.min(width, finalWidth);
                makeNewTextLayout(finalWidth - horizontalPadding);
                break;

            case View.MeasureSpec.EXACTLY:
                finalWidth = width;
                makeNewTextLayout(finalWidth - horizontalPadding);
                break;

            case View.MeasureSpec.UNSPECIFIED:
                finalWidth = Math.max(getSuggestedMinimumWidth(),
                        (int) Math.ceil(Layout.getDesiredWidth(mText, mTextPaint)) + horizontalPadding);
                makeNewTextLayout(finalWidth - horizontalPadding);
                break;
        }

        switch (heightMode) {
            case View.MeasureSpec.AT_MOST:
                finalHeight = Math.max(getSuggestedMinimumHeight(),
                        mTextLayout.getHeight() + verticalPadding);
                finalHeight = Math.min(height, finalHeight);
                break;

            case View.MeasureSpec.EXACTLY:
                finalHeight = height;
                break;

            case View.MeasureSpec.UNSPECIFIED:
                finalHeight = Math.max(getSuggestedMinimumHeight(),
                        mTextLayout.getHeight() + verticalPadding);
                break;
        }

        setMeasuredDimension(finalWidth, finalHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (!mScroller.isFinished())
            mScroller.forceFinished(true);

        if (mTextLayout == null)
            return;

        if (hasSelection()) {
            mHandleMgr.showLeftHandle();
            mHandleMgr.showRightHandle();
            mSelectionMgr.drawSelectionHighlight();

            stopTextActionMode();
            startTextActionMode();
        }

        scrollToFit(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.save();
        canvas.translate(getPaddingLeft(), getPaddingTop());
        mTextLayout.draw(canvas,
                mSelectionMgr.mSelectionPath, mSelectionMgr.mSelectionPathPaint, 0);
        canvas.restore();

        drawVerticalEdgeEffect(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean superResult = super.onTouchEvent(event);
        boolean gestureResult = mGestureDetector.onTouchEvent(event);

        return superResult || gestureResult;
    }


    public void setText(CharSequence text) {
        if (TextUtils.isEmpty(text))
            text = "";

        mText = text;
        makeNewTextLayout();

        if (!mScroller.isFinished())
            mScroller.forceFinished(true);

        mHandleMgr.closeAllHandle();
        mSelectionMgr.eraseSelectionHighlight();
        mSelectionMgr.setSelection(0);
        stopTextActionMode();

        scrollTo(0, 0);

        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        if (mTextColor == color) return;

        mTextColor = color;
        mTextPaint.setColor(mTextColor);

        invalidate();
    }

    public void setTextSize(float size) {
        float textSizeDip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size,
                getContext().getResources().getDisplayMetrics());

        if (mTextSize == textSizeDip) return;

        mTextSize = textSizeDip;
        mTextPaint.setTextSize(textSizeDip);

        makeNewTextLayout();

        if (hasSelection()) {
            mHandleMgr.showLeftHandle();
            mHandleMgr.showRightHandle();
            mSelectionMgr.drawSelectionHighlight();

            stopTextActionMode();
            startTextActionMode();
        }

        scrollToFit(false);

        requestLayout();
        invalidate();
    }

    public void setLineSpacing(float mult, float add) {
        float addDip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, add,
                getContext().getResources().getDisplayMetrics());

        if (mult == mLineSpacingMult && addDip == mLineSpacingAdd)
            return;

        mLineSpacingAdd = addDip;
        mLineSpacingMult = mult;

        makeNewTextLayout();

        if (hasSelection()) {
            mHandleMgr.showLeftHandle();
            mHandleMgr.showRightHandle();
            mSelectionMgr.drawSelectionHighlight();

            stopTextActionMode();
            startTextActionMode();
        }

        scrollToFit(false);

        requestLayout();
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        mTextPaint.setTypeface(typeface);

        scrollToFit(false);

        requestLayout();
        invalidate();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener l) {
        mSelectionListener = l;
    }


    /**
     * 设置位于屏幕左上方的文本位置（偏移）。该方法只在初始化时有效，若在调用此方法后又调用了
     * {@link #computeTextOffsetAtViewTop()} 方法，此方法设置的值将被刷新。
     * 默认值为0。
     */
    public void setTextOffsetAtViewTop(int textOffset) {
        mTextOffsetAtViewTop = textOffset;
        mOffsetFromTextOffset = 0;
    }

    public void setTextOffsetAtViewTop(int textOffset, int offset) {
        mTextOffsetAtViewTop = textOffset;
        mOffsetFromTextOffset = offset;
    }


    /**
     * 计算此时位于屏幕左上方的文本位置（偏移），并记录此位置。
     * 使用 {@link #getTextOffsetAtViewTop()} 可获得该值。
     * 该方法在 {@link #mScrollChangedListener} 中被调用以实现实时记录。
     */
    public void computeTextOffsetAtViewTop() {
        int curLine = mTextLayout.getLineForVertical(getScrollY() - getPaddingTop());
        int lineTop = mTextLayout.getLineTop(curLine);
        mTextOffsetAtViewTop = mTextLayout.getOffsetForHorizontal(curLine, 0f);
        mOffsetFromTextOffset = getScrollY() - getPaddingTop() - lineTop;
    }

    public void scrollToBottom(boolean smoothScroll) {
        scrollToTextOffset(Math.max(mText.length() - 1, 0), 0, smoothScroll);
    }

    public void scrollToTextOffset(int textOffset, int offset, boolean smoothScroll) {
        mTextOffsetAtViewTop = textOffset;
        mOffsetFromTextOffset = offset;

        if (mTextLayout == null)
            return;

        int yOffset = getScrollOffsetFromTextOffset(textOffset) + offset;
        if (yOffset == 0) {
            return;
        }

        //检查是否超出屏幕。
        int maxScrollOffset = mTextLayout.getHeight()
                + getPaddingTop() + getPaddingBottom()
                - (getHeight() + getScrollY());
        yOffset = Math.min(maxScrollOffset, yOffset);
        if (yOffset == 0) {
            return;
        }

        if (smoothScroll) {
            mScroller.startScroll(getScrollX(), getScrollY(), 0, yOffset);
            invalidate();
        } else {
            scrollBy(0, yOffset);
        }
    }

    /**
     * 获得最近一次记录的位于屏幕左上方的文本位置（偏移）。
     * 使用 {@link #computeTextOffsetAtViewTop()} 可刷新此值。
     * 默认值为0。
     */
    public int getTextOffsetAtViewTop() {
        return mTextOffsetAtViewTop;
    }

    public int getOffsetFromCurTextOffset() {
        return mOffsetFromTextOffset;
    }

    public int getScrollOffsetFromTextOffset(int textOffset) {
        int finalOffsetY;

        int line = mTextLayout.getLineForOffset(textOffset);
        if (line == 0)
            finalOffsetY = -getScrollY();
        else {
            int lineTop = mTextLayout.getLineTop(line);
            finalOffsetY = lineTop + getPaddingTop() - getScrollY();
        }

        return finalOffsetY;
    }


    public int getSelectionStart() {
        return mSelectionMgr.getSelectionStart();
    }

    public int getSelectionEnd() {
        return mSelectionMgr.getSelectionEnd();
    }

    public String getSelectedText() {
        if (!hasSelection())
            return null;

        return String.valueOf(mText.subSequence(getSelectionStart(), getSelectionEnd()));
    }

    public CharSequence getText() {
        return mText;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public boolean hasSelection() {
        return mSelectionMgr.getSelectionEnd()
                != mSelectionMgr.getSelectionStart();
    }


    private void onStartScroll() {
        if (hasSelection()) {
            mHandleMgr.closeAllHandle();

            stopTextActionMode();
        }
    }

    private void onScrolling() {
        if (mTextLayout != null)
            computeTextOffsetAtViewTop();
    }

    private void onEndScroll() {
        if (hasSelection() && !mGestureDetector.isLongPressed()) {
            mHandleMgr.showLeftHandle(true);
            mHandleMgr.showRightHandle(true);

            startTextActionMode();
        }
    }


    private Runnable mScrollMonitorRunnable = new Runnable() {
        @Override
        public void run() {
            if (mIsFingerOnScreen) {
                removeCallbacks(mScrollMonitorRunnable);
                postDelayed(mScrollMonitorRunnable, 100);
            } else {
                onEndScroll();
                mIsScrolling = false;
            }
        }
    };

    private ViewTreeObserver.OnScrollChangedListener mScrollChangedListener
            = new ViewTreeObserver.OnScrollChangedListener() {
        @Override
        public void onScrollChanged() {
            if (!mIsScrolling) {
                onStartScroll();
                mIsScrolling = true;
            }

            removeCallbacks(mScrollMonitorRunnable);
            postDelayed(mScrollMonitorRunnable, 100);

            onScrolling();
        }
    };

    private TextGestureDetector.Callback mTextGDCallback = new TextGestureDetector.Callback() {
        private int mNestedYOffset;
        private MotionEvent mVtev;

        private int[] mScrollConsumed = new int[2];
        private int[] mScrollOffset = new int[2];


        @Override
        public void onStartEvent(MotionEvent e) {
            mVtev = MotionEvent.obtain(e);
            if (e.getAction() == MotionEvent.ACTION_DOWN)
                mNestedYOffset = 0;
            mVtev.offsetLocation(0, mNestedYOffset);
        }

        @Override
        public void onEndEvent(MotionEvent e, VelocityTracker velocityTracker, boolean result) {
            if (velocityTracker != null)
                velocityTracker.addMovement(mVtev);

            mVtev.recycle();
        }

        @Override
        public boolean onDown(MotionEvent e) {
            if (!mScroller.isFinished())
                mScroller.forceFinished(true);

            mIsFingerOnScreen = true;
            startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
            return true;
        }

        @Override
        public void onStartUp(MotionEvent e, VelocityTracker velocityTracker) {
            if (velocityTracker != null)
                velocityTracker.addMovement(mVtev);
        }

        @Override
        public boolean onUp(MotionEvent e) {
            stopNestedScroll();
            mIsFingerOnScreen = false;
            return true;
        }

        @Override
        public void onCancel(MotionEvent e) {
            stopNestedScroll();
            mIsFingerOnScreen = false;
        }

        @Override
        public boolean onDetermineCanScroll(float xDiff, float yDiff, int touchSlop) {
            return yDiff > touchSlop && yDiff > xDiff;
        }

        @Override
        public boolean onSingleTap(MotionEvent e) {
            mHandleMgr.closeAllHandle();
            mSelectionMgr.eraseSelectionHighlight();
            mSelectionMgr.setSelection(0);
            stopTextActionMode();
            return false;
        }

        @Override
        public void onDoubleTap(MotionEvent e) {
            float textX = convertToTextX(e.getX());
            float textY = convertToTextY(e.getY());
            int touchedLine = mTextLayout.getLineForVertical((int) textY);
            int touchedOffset = mTextLayout.getOffsetForHorizontal(touchedLine, textX);

            boolean isHitText = (textX >= mTextLayout.getLineLeft(touchedLine))
                    && (textX <= mTextLayout.getLineRight(touchedLine))
                    && (textY >= mTextLayout.getLineTop(touchedLine))
                    && (textY <= mTextLayout.getLineBottom(touchedLine));

            if (isHitText) {
                long range = mTextIterator.selectWord(touchedOffset);
                int startSelection = unpackRangeStart(range);
                int endSelection = unpackRangeEnd(range);
                if (startSelection < 0 || endSelection < 0)
                    return;

                mSelectionMgr.setSelection(startSelection, endSelection);
                mSelectionMgr.drawSelectionHighlight();

                if (!scrollToFit(true)) {
                    mHandleMgr.showLeftHandle();
                    mHandleMgr.showRightHandle();
                    startTextActionMode();
                }
            }
        }

        @Override
        public boolean onLongTapping(boolean first, float prevTouchedX, float prevTouchedY) {
            float textX = convertToTextX(prevTouchedX);
            float textY = convertToTextY(prevTouchedY);
            int touchedLine = mTextLayout.getLineForVertical((int) textY);
            int touchedOffset = mTextLayout.getOffsetForHorizontal(touchedLine, textX);

            boolean isHitText = (textX >= mTextLayout.getLineLeft(touchedLine))
                    && (textX <= mTextLayout.getLineRight(touchedLine))
                    && (textY >= mTextLayout.getLineTop(touchedLine))
                    && (textY <= mTextLayout.getLineBottom(touchedLine));

            if (isHitText) {
                long range = mTextIterator.selectWord(touchedOffset);
                int startSelection = unpackRangeStart(range);
                int endSelection = unpackRangeEnd(range);
                if (startSelection < 0 || endSelection < 0)
                    return false;

                if (first) {
                    stopTextActionMode();
                    mHandleMgr.closeAllHandle();

                    mSelectionMgr.setSelection(startSelection, endSelection);
                    mSelectionMgr.drawSelectionHighlight();

                    mLongTapSelectionInitEnd = endSelection;
                    mLongTapSelectionInitStart = startSelection;
                } else {
                    if (touchedOffset > mLongTapSelectionInitEnd) {
                        mSelectionMgr.setSelection(mLongTapSelectionInitStart, endSelection);
                        mSelectionMgr.drawSelectionHighlight();
                    } else if (touchedOffset < mLongTapSelectionInitStart) {
                        mSelectionMgr.setSelection(startSelection, mLongTapSelectionInitEnd);
                        mSelectionMgr.drawSelectionHighlight();
                    }
                }

                scrollToFit(true);

                return true;
            }

            return false;
        }

        @Override
        public void onLongTap(MotionEvent e) {
            mHandleMgr.showLeftHandle(true);
            mHandleMgr.showRightHandle(true);
            startTextActionMode();
        }

        @Override
        public void onStartScroll(MotionEvent e) {
            final ViewParent parent = getParent();
            if (parent != null)
                parent.requestDisallowInterceptTouchEvent(true);
        }

        @Override
        public void onScroll(MotionEvent e,
                             float startX, float startY,
                             float curX, float curY,
                             float dx, float dy) {
            int dxInt = (int) -dx;
            int dyInt = (int) -dy;

            if (dispatchNestedPreScroll(dxInt, dyInt, mScrollConsumed, mScrollOffset)) {
                dxInt -= mScrollConsumed[0];
                dyInt -= mScrollConsumed[1];

                mVtev.offsetLocation(0, mScrollOffset[1]);
                mNestedYOffset += mScrollOffset[1];
                mGestureDetector.setPrevY(
                        mGestureDetector.getPrevY() - mScrollOffset[1]);
            }

            int actuallyDy = -processVerticalMove(-dyInt);

            if (actuallyDy != 0) {
                final int range = getTextHeightWithPadding();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && range > getHeight());

                if (canOverscroll) {
                    if (-dyInt + getScrollY() < 0) {
                        mEdgeGlowTop.onPull(dy / getHeight(), 0.5f + (curX - startX) / (getWidth() * 2));
                        if (!mEdgeGlowBottom.isFinished())
                            mEdgeGlowBottom.onRelease();
                    } else if (-dyInt + getScrollY() + getHeight() > range) {
                        mEdgeGlowBottom.onPull(dy / getHeight(), 0.5f - (curX - startY) / (getWidth() * 2));
                        if (!mEdgeGlowTop.isFinished())
                            mEdgeGlowTop.onRelease();
                    }
                }
                if (mEdgeGlowTop != null
                        && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished()))
                    postInvalidateOnAnimation();
            }

            if (dispatchNestedScroll(0, actuallyDy, dxInt, dyInt - actuallyDy, mScrollOffset)) {
                mGestureDetector.setPrevY(
                        mGestureDetector.getPrevY() - mScrollOffset[1]);
                mVtev.offsetLocation(0, mScrollOffset[1]);
                mNestedYOffset += mScrollOffset[1];
            }
        }

        @Override
        public void onFling(float velocityX, float velocityY) {
            int maxY = getTextHeightWithPadding() - getHeight();

            final int scrollY = getScrollY();
            final boolean canFling = (scrollY > 0 || velocityY > 0)
                    && (scrollY < getVerticalScrollRange() || velocityY < 0);

            Log.d("onFling", "velocityY:" + velocityY + " canFling:" + canFling);

            if (!dispatchNestedPreFling(0, velocityY)) {
                Log.d("onFling", "dispatchNestedPreFling():false");

                dispatchNestedFling(0, velocityY, canFling);
                if (canFling) {
                    mScroller.fling(getScrollX(), getScrollY(),
                            (int) velocityX, (int) velocityY,
                            0, 0, 0, Math.max(0, maxY));
                    ViewCompat.postInvalidateOnAnimation(StaticTextView.this);
                }
            }
        }
    };


    private class TextIterator {
        private BreakIterator mWordIterator;

        public TextIterator() {
            mWordIterator = BreakIterator.getWordInstance();
        }

        public long selectWord(int position) {
            mWordIterator.setText(mText.toString());
            int end = mWordIterator.following(position);
            int start = mWordIterator.previous();

            return packRange(start, end);
        }
    }

    private class TextIntentProcessor {
        private PackageManager mPackageManager;

        TextIntentProcessor() {
            mPackageManager = getContext().getPackageManager();
        }

        private Intent createProcessTextIntent() {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PROCESS_TEXT);
            intent.setType("text/plain");
            return intent;
        }

        private Intent createProcessTextIntentForResolveInfo(ResolveInfo resolveInfo) {
            Intent intent = createProcessTextIntent();
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, getSelectedText());
            intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);
            // 从 Android 7 引入的 bug 导致不加该标志也能正常工作；从 Android 9 开始强制要求该标志
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            return intent;
        }

        private List<ResolveInfo> getSupportedActivities() {
            return mPackageManager.queryIntentActivities(
                    createProcessTextIntent(),
                    PackageManager.MATCH_DEFAULT_ONLY);
        }

        private CharSequence getLabel(ResolveInfo resolveInfo) {
            return resolveInfo.loadLabel(mPackageManager);
        }

        void fetchTextProcessMenu(Menu menu) {
            for (ResolveInfo resolveInfo : getSupportedActivities()) {
                if (resolveInfo.activityInfo.packageName.equals("com.baidu.BaiduMap") ||
                        resolveInfo.activityInfo.packageName.equals("com.microsoft.office.officehub") ||
                        resolveInfo.activityInfo.packageName.equals("com.microsoft.office.outlook") ||
                        resolveInfo.activityInfo.packageName.equals("com.microsoft.todos"))
                {
                    continue;
                }

                menu.add(Menu.NONE, Menu.NONE, MENU_ITEM_ORDER_TEXT_PROCESS, getLabel(resolveInfo))
                        .setIntent(createProcessTextIntentForResolveInfo(resolveInfo))
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        }
    }

    private class SelectionManager {
        private int mSelStart;
        private int mSelEnd;

        Path mSelectionPath;
        Paint mSelectionPathPaint;

        private Rect mRedrawBounds;
        private RectF mSelectionBounds;


        SelectionManager() {
            mSelStart = -1;
            mSelEnd = -1;

            mSelectionPath = new Path();
            mSelectionPath.reset();

            mRedrawBounds = new Rect();
            mSelectionBounds = new RectF();

            mSelectionPathPaint = new Paint();

            TypedArray a = getContext().obtainStyledAttributes(
                    new int[]{android.R.attr.textColorHighlight});
            int highlightColor = a.getColor(a.getIndex(0), Color.parseColor("#1e88e5"));
            a.recycle();

            mSelectionPathPaint.setColor(highlightColor);
        }

        void setSelection(int selection) {
            setSelection(selection, selection);
        }

        void setSelection(int start, int end) {
            int oldStart = mSelStart;
            int oldEnd = mSelEnd;

            mSelStart = start;
            mSelEnd = end;

            if (mSelectionListener != null
                    && (start != oldStart || end != oldEnd))
                mSelectionListener.onSelectionChanged(start, end, oldStart, oldEnd);
        }

        int getSelectionStart() {
            return mSelStart;
        }

        int getSelectionEnd() {
            return mSelEnd;
        }

        void drawSelectionHighlight() {
            drawSelectionHighlight(mSelStart, mSelEnd);
        }

        void drawSelectionHighlight(int start, int end) {
            if (start < 0 || end < 0 || start == end)
                return;

            mTextLayout.getSelectionPath(start, end, mSelectionPath);
            //relocateSelectionPath(mSelectionPath);
            //computeSelectionPath(start, end, mSelectionPath);

            mSelectionPath.computeBounds(mSelectionBounds, true);
            mRedrawBounds.set(
                    (int) Math.floor(mSelectionBounds.left + getPaddingLeft()),
                    (int) Math.floor(mSelectionBounds.top + getPaddingTop()),
                    (int) Math.ceil(mSelectionBounds.right + getPaddingLeft()),
                    (int) Math.ceil(mSelectionBounds.bottom + getPaddingTop()));

            invalidate(mRedrawBounds);
        }

        void eraseSelectionHighlight() {
            eraseSelectionHighlight(mSelStart, mSelEnd);
        }

        void eraseSelectionHighlight(int start, int end) {
            if (start < 0 || end < 0 || start == end)
                return;

            mTextLayout.getSelectionPath(start, end, mSelectionPath);
            //relocateSelectionPath(mSelectionPath);
            //computeSelectionPath(start, end, mSelectionPath);

            mSelectionPath.computeBounds(mSelectionBounds, true);
            mRedrawBounds.set(
                    (int) Math.floor(mSelectionBounds.left + getPaddingLeft()),
                    (int) Math.floor(mSelectionBounds.top + getPaddingTop()),
                    (int) Math.ceil(mSelectionBounds.right + getPaddingLeft()),
                    (int) Math.ceil(mSelectionBounds.bottom + getPaddingTop()));

            mSelectionPath.reset();
            invalidate(mRedrawBounds);
        }
    }

    private class HandleManager {
        private TextHandle mLeftHandle;
        private TextHandle mRightHandle;
        private HandleEventListener mListener;

        private Path mSelectionPath;
        private RectF mSelectionBounds;

        private final int[] mViewLocationOnScreen = new int[2];
        private final int[] mWindowLocationOnScreen = new int[2];


        HandleManager() {
            mListener = new HandleEventListener();

            mLeftHandle = new TextHandle(
                    getContext(), TextHandle.HandleType.LEFT);
            mLeftHandle.setHandleEventListener(mListener);

            mRightHandle = new TextHandle(
                    getContext(), TextHandle.HandleType.RIGHT);
            mRightHandle.setHandleEventListener(mListener);

            mSelectionPath = new Path();
            mSelectionPath.reset();
            mSelectionBounds = new RectF();
        }

        private int getLineBottomWithNoSpaceing(int selStart, int selEnd) {
            mTextLayout.getSelectionPath(selStart, selEnd, mSelectionPath);
            mSelectionPath.computeBounds(mSelectionBounds, true);
            mSelectionPath.reset();
            return (int) Math.ceil(mSelectionBounds.bottom);
        }

        private void calLeftHandleLocation(int[] outLocation) {
            int selectionStart = mSelectionMgr.getSelectionStart();

            getLocationOnScreen(mViewLocationOnScreen);

            float startOffsetX = mTextLayout.getPrimaryHorizontal(selectionStart);
            float startOffsetLineBottom;
            if (selectionStart == 0) {
                startOffsetLineBottom = getLineBottomWithNoSpaceing(selectionStart, selectionStart + 1);
            } else {
                startOffsetLineBottom = getLineBottomWithNoSpaceing(selectionStart - 1, selectionStart);
            }

            int x = (int) (mViewLocationOnScreen[0] + getPaddingLeft()
                    + startOffsetX - getScrollX()
                    - mLeftHandle.getWidth() * 3 / 4f);

            int y = (int) (mViewLocationOnScreen[1] + getPaddingTop()
                    - getScrollY()
                    + startOffsetLineBottom);

            outLocation[0] = x;
            outLocation[1] = y;
        }

        private void calRightHandleLocation(int[] outLocation) {
            int selectionEnd = mSelectionMgr.getSelectionEnd();

            getLocationOnScreen(mViewLocationOnScreen);

            float endOffsetX = mTextLayout.getPrimaryHorizontal(selectionEnd);
            float endOffsetLineBottom = getLineBottomWithNoSpaceing(selectionEnd - 1, selectionEnd);

            int x = (int) (mViewLocationOnScreen[0] + getPaddingLeft()
                    + endOffsetX - getScrollX()
                    - mRightHandle.getWidth() / 4f);

            int y = (int) (mViewLocationOnScreen[1] + getPaddingTop()
                    - getScrollY()
                    + endOffsetLineBottom);

            outLocation[0] = x;
            outLocation[1] = y;
        }

        boolean showLeftHandle() {
            return showLeftHandle(false);
        }

        boolean showLeftHandle(boolean forceUpdate) {
            if (!isLeftHandleBeyond()) {
                calLeftHandleLocation(mWindowLocationOnScreen);

                int x = mWindowLocationOnScreen[0];
                int y = mWindowLocationOnScreen[1];
                mLeftHandle.show(StaticTextView.this, x, y, forceUpdate);

                return true;
            } else {
                mLeftHandle.close();
                return false;
            }
        }

        boolean showRightHandle() {
            return showRightHandle(false);
        }

        boolean showRightHandle(boolean forceUpdate) {
            if (!isRightHandleBeyond()) {
                calRightHandleLocation(mWindowLocationOnScreen);

                int x = mWindowLocationOnScreen[0];
                int y = mWindowLocationOnScreen[1];
                mRightHandle.show(StaticTextView.this, x, y, forceUpdate);

                return true;
            } else {
                mRightHandle.close();
                return false;
            }
        }

        void closeAllHandle() {
            closeLeftHandle();
            closeRightHandle();
        }

        void closeLeftHandle() {
            mLeftHandle.close();
        }

        void closeRightHandle() {
            mRightHandle.close();
        }

        boolean isLHShowing() {
            return mLeftHandle.isShowing();
        }

        boolean isRHShowing() {
            return mRightHandle.isShowing();
        }

        boolean isLeftHandleBeyond() {
            int selectionStart = mSelectionMgr.getSelectionStart();

            int offsetLine = mTextLayout.getLineForOffset(selectionStart);
            float offsetX = mTextLayout.getPrimaryHorizontal(selectionStart);
            float offsetLineBottom = mTextLayout.getLineBottom(offsetLine);

            int x = (int) (offsetX - getScrollX());
            int y = (int) (offsetLineBottom - getScrollY());

            return ((x < -getPaddingLeft() || x > getWidth() - getPaddingRight())
                    || (y < -getPaddingTop() || y > getHeight() - getPaddingBottom()));
        }

        boolean isRightHandleBeyond() {
            int selectionEnd = mSelectionMgr.getSelectionEnd();

            int offsetLine = mTextLayout.getLineForOffset(selectionEnd);
            float offsetX = mTextLayout.getPrimaryHorizontal(selectionEnd);
            float offsetLineBottom = mTextLayout.getLineBottom(offsetLine);

            int x = (int) (offsetX - getScrollX());
            int y = (int) (offsetLineBottom - getScrollY());

            return ((x < -getPaddingLeft() || x > getWidth() - getPaddingRight())
                    || (y < -getPaddingTop() || y > getHeight() - getPaddingBottom()));
        }


        private class HandleEventListener implements TextHandle.HandleListener {
            private float mStartLineY;
            private float mStartScreenY;
            private float mEndLineY;
            private float mEndScreenY;

            private final int[] mViewLocationOnScreen = new int[2];


            private boolean determineLeftHandle(float textLayoutX, float textLayoutY) {
                int curLine = mTextLayout.getLineForVertical((int) textLayoutY);
                int curOffset = mTextLayout.getOffsetForHorizontal(curLine, textLayoutX);

                int newStartOffset;
                int newEndOffset;
                int prevStartOffset = mSelectionMgr.getSelectionStart();
                int prevEndOffset = mSelectionMgr.getSelectionEnd();

                if (curOffset >= prevEndOffset) {
                    newStartOffset = prevEndOffset - 1;
                    newEndOffset = prevEndOffset;
                } else {
                    newStartOffset = curOffset;
                    newEndOffset = prevEndOffset;
                }

                if (newStartOffset == -1 || newEndOffset == -1)
                    return false;

                mSelectionMgr.setSelection(newStartOffset, newEndOffset);
                mSelectionMgr.drawSelectionHighlight();
                mHandleMgr.showLeftHandle();
                return true;
            }


            private boolean determineRightHandle(float textLayoutX, float textLayoutY) {
                int curLine = mTextLayout.getLineForVertical((int) textLayoutY);
                int curOffset = mTextLayout.getOffsetForHorizontal(curLine, textLayoutX);

                int newStartOffset;
                int newEndOffset;
                int prevStartOffset = mSelectionMgr.getSelectionStart();
                int prevEndOffset = mSelectionMgr.getSelectionEnd();

                if (curOffset <= prevStartOffset) {
                    newStartOffset = prevStartOffset;
                    newEndOffset = prevStartOffset + 1;
                } else {
                    newStartOffset = prevStartOffset;
                    newEndOffset = curOffset;
                }

                if (newStartOffset == -1 || newEndOffset == -1)
                    return false;

                mSelectionMgr.setSelection(newStartOffset, newEndOffset);
                mSelectionMgr.drawSelectionHighlight();
                mHandleMgr.showRightHandle();
                return true;
            }


            @Override
            public void onCapture(TextHandle view, MotionEvent e) {
                int curStartLine = mTextLayout.getLineForOffset(mSelectionMgr.getSelectionStart());
                mStartLineY = (mTextLayout.getLineBottom(curStartLine) + mTextLayout.getLineTop(curStartLine)) / 2f;

                int curEndLine = mTextLayout.getLineForOffset(mSelectionMgr.getSelectionEnd());
                mEndLineY = (mTextLayout.getLineBottom(curEndLine) + mTextLayout.getLineTop(curEndLine)) / 2f;

                mEndScreenY = e.getRawY();
                mStartScreenY = e.getRawY();
            }

            @Override
            public void onDrag(TextHandle view, MotionEvent e) {
                getLocationOnScreen(mViewLocationOnScreen);

                float viewX = e.getRawX() - mViewLocationOnScreen[0];
                float viewY = e.getRawY() - mViewLocationOnScreen[1];
                float textLayoutX = viewX - getPaddingLeft() + getScrollX();
                float textLayoutY = viewY - getPaddingTop() + getScrollY();

                switch (view.getType()) {
                    case LEFT: {
                        stopTextActionMode();
                        determineLeftHandle(textLayoutX, mStartLineY + (e.getRawY() - mStartScreenY));
                        break;
                    }

                    case RIGHT: {
                        stopTextActionMode();
                        determineRightHandle(textLayoutX, mEndLineY + (e.getRawY() - mEndScreenY));
                        break;
                    }
                }
            }

            @Override
            public void onClick(TextHandle view, MotionEvent e) {
            }

            @Override
            public void onRelease(TextHandle view, MotionEvent e) {
                switch (view.getType()) {
                    case LEFT:
                        startTextActionMode();
                        break;

                    case RIGHT:
                        startTextActionMode();
                        break;
                }
            }
        }
    }


    private boolean canCopy() {
        return hasSelection();
    }

    private boolean canSelectAll() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        if (selectionEnd < selectionStart) {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        return !(selectionStart == 0 && selectionEnd == mText.length());
    }

    private boolean canWebSearch() {
        return hasSelection();
    }

    private void performCopy() {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();

        mClipMgr.setPrimaryClip(
                ClipData.newPlainText(null,
                        mText.subSequence(selectionStart, selectionEnd)));

        stopTextActionMode();
        mHandleMgr.closeAllHandle();
        mSelectionMgr.eraseSelectionHighlight();
        mSelectionMgr.setSelection(0);
    }

    private void performSelectAll() {
        mSelectionMgr.setSelection(0, mText.length());
        mSelectionMgr.drawSelectionHighlight();

        mHandleMgr.showLeftHandle();
        mHandleMgr.showRightHandle();

        if (mTextActionMode != null)
            mTextActionMode.invalidateContentRect();
    }

    private void performWebSearch() {
        SharedPreferences preferences
                = PreferenceManager.getDefaultSharedPreferences(getContext());
        String wsKey = preferences.getString("search_url", getContext().getString(R.string.default_search_url));
        if (wsKey.contains("$1"))
            wsKey = wsKey.replace("$1", getSelectedText());
        else
            wsKey = wsKey + getSelectedText();

        UApplication.chromeCustomTabsManager
                .startChromeTab(getContext(), wsKey);
    }

    private ActionMode.Callback getActionModeCallback() {
        return new ActionMode.Callback2() {
            private RectF mContentBounds = new RectF();

            private boolean updateSelectAllMenuItem(Menu menu) {
                boolean canSelectAll = canSelectAll();
                boolean selectAllItemExistes = menu.findItem(MENU_ITEM_ID_SELECT_ALL) != null;
                if (canSelectAll && !selectAllItemExistes) {
                    menu.add(Menu.NONE, MENU_ITEM_ID_SELECT_ALL, MENU_ITEM_ORDER_SELECT_ALL, "全选").
                            setAlphabeticShortcut('a').
                            setIcon(R.drawable.ic_action_select_all).
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
                } else if (!canSelectAll && selectAllItemExistes)
                    menu.removeItem(MENU_ITEM_ID_SELECT_ALL);
                else
                    return false;

                return true;
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                mode.setTitle("选择文本");
                mode.setSubtitle(null);
                mode.setTitleOptionalHint(true);

                boolean canCreate = false;

                if (canCopy()) {
                    menu.add(Menu.NONE, MENU_ITEM_ID_COPY, MENU_ITEM_ORDER_COPY, "复制").
                            setAlphabeticShortcut('c').
                            setIcon(R.drawable.ic_action_copy).
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                    canCreate = true;
                }

                updateSelectAllMenuItem(menu);

                if (canSelectAll())
                    canCreate = true;

                if (canWebSearch()) {
                    menu.add(Menu.NONE, MENU_ITEM_ID_WEB_SEARCH, MENU_ITEM_ORDER_WEB_SEARCH, "网页搜索").
                            setAlphabeticShortcut('s').
                            setIcon(R.drawable.ic_search).
                            setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                    canCreate = true;
                }

                mTextIntentProcessor.fetchTextProcessMenu(menu);
                return canCreate;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return updateSelectAllMenuItem(menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_ITEM_ID_COPY:
                        performCopy();
                        return true;

                    case MENU_ITEM_ID_SELECT_ALL:
                        performSelectAll();
                        return true;

                    case MENU_ITEM_ID_WEB_SEARCH:
                        performWebSearch();
                        return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                mTextActionMode = null;
            }

            @Override
            public void onGetContentRect(ActionMode mode, View view, Rect outRect) {
                if (hasSelection()) {
                    mContentBounds.set(mSelectionMgr.mSelectionBounds);
                    if (mHandleMgr.isRHShowing())
                        mContentBounds.bottom += mHandleMgr.mRightHandle.getHeight();
                }

                // Take TextView's padding and scroll into account.
                int textHorizontalOffset = getPaddingLeft() - getScrollX();
                int textVerticalOffset = getPaddingTop() - getScrollY();
                outRect.set(
                        (int) Math.floor(mContentBounds.left + textHorizontalOffset),
                        (int) Math.floor(mContentBounds.top + textVerticalOffset),
                        (int) Math.ceil(mContentBounds.right + textHorizontalOffset),
                        (int) Math.ceil(mContentBounds.bottom + textVerticalOffset));
            }
        };

    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int newStart, int newEnd, int oldStart, int oldEnd);
    }
}