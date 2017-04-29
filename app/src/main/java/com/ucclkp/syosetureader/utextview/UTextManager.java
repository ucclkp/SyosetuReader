package com.ucclkp.syosetureader.utextview;

import android.app.Activity;
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
import android.graphics.Rect;
import android.graphics.RectF;
import android.preference.PreferenceManager;
import android.text.DynamicLayout;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.SpanWatcher;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.chromecustomtabs.ChromeCustomTabsManager;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class UTextManager
{
    private int mMaxWidth;
    private boolean mIsAutoWidth;
    private int mActionStyle = FLOAT;

    private UTextView mTargetView;

    private Paint mTestPaint;
    private TextPaint mTextPaint;
    private SpannableStringBuilder mBaseText;
    private int mTextDefColor;
    private int mTextLinkColor;
    private float mTextDefSize;
    private Layout.Alignment mTextDefAlignment;

    private DynamicLayout mTextLayout;
    private ActionMode mTextActionMode;
    private UTextActionModeCallback mTextActionModeCallback;
    private UAssistantManager mAssistMgr;
    private ClipboardManager mClipMgr;

    private TextIterator mTextIterator;
    private TextIntentProcessor mTextIntentProcessor;
    private ChromeCustomTabsManager mCCTabsManager;

    private boolean mIsTextActionModeStopTemporary;

    private int mTextOffsetAtViewTop;
    private int mLongClickingSelectionStart;
    private int mLongClickingSelectionEnd;
    private int mLongClickingSelectionInitStart;
    private int mLongClickingSelectionInitEnd;


    private enum DrawReqReason
    {
        FULL,
        TEXT_CHANGED,
    }

    private enum LayoutReqReason
    {
        ENFORCE,
        TEXT_CHANGED,
    }

    public final static int FLOAT = 1;
    public final static int PRIMARY = 2;

    private final static int MENU_ITEM_ID_COPY = 1;
    private final static int MENU_ITEM_ID_SELECT_ALL = 4;
    private final static int MENU_ITEM_ID_WEB_SEARCH = 5;
    private final static int MENU_ITEM_ID_TRANSLATE = 6;

    private final static int MENU_ITEM_ORDER_COPY = 1;
    private final static int MENU_ITEM_ORDER_SELECT_ALL = 4;
    private final static int MENU_ITEM_ORDER_WEB_SEARCH = 5;
    private final static int MENU_ITEM_ORDER_TRANSLATE = 100;

    private final static int PROCESS_TEXT_REQUEST_CODE = 1010;


    public UTextManager(UTextView targetView)
    {
        mTargetView = targetView;

        mBaseText = new SpannableStringBuilder("");
        mBaseText.setSpan(new UEditWatcher(), 0, mBaseText.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);

        mIsTextActionModeStopTemporary = false;

        mMaxWidth = 0;
        mIsAutoWidth = false;
        mTextOffsetAtViewTop = 0;

        mTextDefSize = 16f;
        mTextDefColor = Color.BLACK;
        mTextDefAlignment = Layout.Alignment.ALIGN_NORMAL;

        mTestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTestPaint.setColor(Color.BLACK);
        mTestPaint.setStrokeWidth(4);

        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

        TypedArray a = mTargetView.getContext().obtainStyledAttributes(
                new int[]{android.R.attr.textColorPrimary, R.attr.colorAccent});
        mTextDefColor = a.getColor(a.getIndex(0), 0);
        mTextLinkColor = a.getColor(a.getIndex(1), 0);
        a.recycle();

        mTextPaint.setColor(mTextDefColor);
        mTextPaint.linkColor = mTextLinkColor;
        mTextPaint.density = targetView.getContext().getResources().getDisplayMetrics().density;
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                mTextDefSize,
                targetView.getContext().getResources().getDisplayMetrics()));

        mTextActionModeCallback = new UTextActionModeCallback();

        mAssistMgr = new UAssistantManager(this);
        mAssistMgr.setHandleEventListener(new HandleEventListener());

        mClipMgr = (ClipboardManager) mTargetView.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        mCCTabsManager = UApplication.chromeCustomTabsManager;

        mTextIterator = new TextIterator();
        mTextIntentProcessor = new TextIntentProcessor();

        makeNewTextLayout((int) Math.ceil(Layout.getDesiredWidth(mBaseText, mTextPaint)));
        Selection.setSelection(mBaseText, mBaseText.length());
    }


    private void makeNewTextLayout(int width)
    {
        if (width <= 0) return;

        if (mTextLayout != null)
        {
            if (mTextLayout.getWidth() == width)
                return;
        }

        mTextLayout = new DynamicLayout(
                mBaseText,
                mTextPaint,
                width,
                mTextDefAlignment,
                1.0f, 0, true);
    }


    private void determineInvalidate(DrawReqReason type)
    {
        switch (type)
        {
            case FULL:
                mTargetView.invalidate();
                break;

            case TEXT_CHANGED:
                mTargetView.invalidate();
                break;
        }
    }

    private boolean determineRequestLayout(LayoutReqReason type)
    {
        switch (type)
        {
            case ENFORCE:
            {
                mTargetView.requestLayout();
                return true;
            }

            case TEXT_CHANGED:
            {
                ViewGroup.LayoutParams lp = mTargetView.getLayoutParams();
                if (lp == null)
                {
                    mTargetView.requestLayout();
                    return true;
                }

                if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT
                        || lp.height == ViewGroup.LayoutParams.WRAP_CONTENT)
                {
                    mTargetView.requestLayout();
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    /**
     * Determine the range of a word around the specific offset.
     * <p><strong>Be sure</strong> param was checked before invoking this method.
     *
     * @param offset position where to obtain the range of a word.
     * @return the range of a word using {@link #packRangeInLong(int, int)} packed.
     */
    long determineWordRange(int offset)
    {
        return mTextIterator.selectWord(offset);
    }

    /**
     * Determine the range of a character around the specific offset.
     * If you use some character coding such as UTF-16, a character need 16bit space.
     * This method can indicate the range of a character at specific offset.
     * <p><strong>Be sure</strong> param was checked before invoking this method.
     *
     * @param offset position where to obtain the range of a character.
     * @return the range of a character using {@link #packRangeInLong(int, int)} packed.
     */
    private long determineCharRange(int offset)
    {
        final int textLength = mBaseText.length();
        if (textLength == 0)
            return packRangeInLong(0, 0);

        if (offset + 1 < textLength)
        {
            final char currentChar = mBaseText.charAt(offset);
            final char nextChar = mBaseText.charAt(offset + 1);
            if (Character.isSurrogatePair(currentChar, nextChar))
            {
                return packRangeInLong(offset, offset + 2);
            }
        }

        if (offset < textLength)
        {
            return packRangeInLong(offset, offset + 1);
        }

        if (offset - 2 >= 0)
        {
            final char previousChar = mBaseText.charAt(offset - 1);
            final char previousPreviousChar = mBaseText.charAt(offset - 2);
            if (Character.isSurrogatePair(previousPreviousChar, previousChar))
            {
                return packRangeInLong(offset - 2, offset);
            }
        }

        if (offset - 1 >= 0)
        {
            return packRangeInLong(offset - 1, offset);
        }

        return packRangeInLong(offset, offset);
    }

    /**
     * Pack 2 int values into a long, useful as a return value for a range
     */
    private long packRangeInLong(int start, int end)
    {
        return (((long) start) << 32) | end;
    }

    /**
     * Get the start value from a range packed in a long by {@link #packRangeInLong(int, int)}
     */
    private int unpackRangeStartFromLong(long range)
    {
        return (int) (range >>> 32);
    }

    /**
     * Get the end value from a range packed in a long by {@link #packRangeInLong(int, int)}
     */
    public static int unpackRangeEndFromLong(long range)
    {
        return (int) (range & 0x00000000FFFFFFFFL);
    }


    private void drawFontMeasureLine(int line, float lineLength, float offsetX, Canvas canvas)
    {
        int baseline = mTextLayout.getLineBaseline(line);
        Paint.FontMetrics fm = mTextPaint.getFontMetrics();

        //baseline.
        mTestPaint.setColor(Color.BLACK);
        canvas.drawLine(
                offsetX, baseline,
                lineLength + offsetX, baseline,
                mTestPaint);

        //ascent.
        mTestPaint.setColor(Color.GREEN);
        canvas.drawLine(
                offsetX, baseline + fm.ascent,
                lineLength + offsetX, baseline + fm.ascent,
                mTestPaint);

        //descent.
        mTestPaint.setColor(Color.RED);
        canvas.drawLine(
                offsetX, baseline + fm.descent,
                lineLength + offsetX, baseline + fm.descent,
                mTestPaint);

        //top.
        mTestPaint.setColor(Color.BLUE);
        canvas.drawLine(
                offsetX, baseline + fm.top,
                lineLength + offsetX, baseline + fm.top,
                mTestPaint);

        //bottom.
        mTestPaint.setColor(Color.YELLOW);
        canvas.drawLine(
                offsetX, baseline + fm.bottom,
                lineLength + offsetX, baseline + fm.bottom,
                mTestPaint);
    }


    public void onDraw(Canvas canvas)
    {
        onDraw(canvas, true);
    }

    public void onDraw(Canvas canvas, boolean drawAssist)
    {
        mTextLayout.draw(canvas,
                mAssistMgr.getSelectionHighlighPath(),
                mAssistMgr.getSelectionHighlighPaint(),
                0);

        /*if (mTextLayout.getLineCount() >= 2)
        {
            drawFontMeasureLine(0, 200f, 0f, canvas);
            drawFontMeasureLine(1, 200f, 205f, canvas);
        }*/
    }

    public void onClicked(float prevTouchedX, float prevTouchedY)
    {
        Log.d("PricipleConfirmer",
                getClass().getName() + "(" + mTargetView.mUUID + ")"
                        + " onClicked()");

        mIsTextActionModeStopTemporary = false;

        if (!mBaseText.toString().equals(""))
        {
            float textX = convertViewXToTextX(prevTouchedX);
            float textY = convertViewYToTextY(prevTouchedY);
            int touchedLine = mTextLayout.getLineForVertical((int) textY);
            int touchedOffset = mTextLayout.getOffsetForHorizontal(touchedLine, textX);

            Selection.setSelection(mBaseText, touchedOffset);
        }

        stopTextActionMode();
    }

    public void onLongClicked()
    {
        Log.d("PricipleConfirmer",
                getClass().getName() + "(" + mTargetView.mUUID + ")"
                        + " onLongClicked()");

        if (mLongClickingSelectionStart != mLongClickingSelectionEnd)
        {
            Selection.setSelection(mBaseText, mLongClickingSelectionStart, mLongClickingSelectionEnd);

            startTextActionMode();
        }
    }

    public boolean onLongClicking(boolean first, float prevTouchedX, float prevTouchedY)
    {
        Log.d("PricipleConfirmer",
                getClass().getName() + "(" + mTargetView.mUUID + ")"
                        + " onLongClicking()");

        float textX = convertViewXToTextX(prevTouchedX);
        float textY = convertViewYToTextY(prevTouchedY);
        int touchedLine = mTextLayout.getLineForVertical((int) textY);
        int touchedOffset = mTextLayout.getOffsetForHorizontal(touchedLine, textX);

        boolean isHitText = (textX >= mTextLayout.getLineLeft(touchedLine)) && (textX <= mTextLayout.getLineRight(touchedLine))
                && (textY >= mTextLayout.getLineTop(touchedLine)) && (textY <= mTextLayout.getLineBottom(touchedLine));

        mIsTextActionModeStopTemporary = false;

        if (!isHitText && first)
        {
            mAssistMgr.closeAllHandle();
            mAssistMgr.eraseSelectionHighlight();
            stopTextActionMode();

            mLongClickingSelectionStart = -1;
            mLongClickingSelectionEnd = -1;
            return false;
        }

        if (first)
        {
            mAssistMgr.closeAllHandle();
            mAssistMgr.eraseSelectionHighlight();

            if (mActionStyle == FLOAT)
                stopTextActionMode();
        } else
        {
            long range = determineWordRange(touchedOffset);
            int startSelection = unpackRangeStartFromLong(range);
            int endSelection = unpackRangeEndFromLong(range);
            if (startSelection < 0 || endSelection < 0)
                return true;

            if (touchedOffset > mLongClickingSelectionInitEnd)
            {
                mLongClickingSelectionEnd = endSelection;
                mLongClickingSelectionStart = mLongClickingSelectionInitStart;
            } else if (touchedOffset < mLongClickingSelectionInitStart)
            {
                mLongClickingSelectionStart = startSelection;
                mLongClickingSelectionEnd = mLongClickingSelectionInitEnd;
            }

            mAssistMgr.drawSelectionHighlight(mLongClickingSelectionStart, mLongClickingSelectionEnd);
            return true;
        }

        long range = determineWordRange(touchedOffset);
        int startSelection = unpackRangeStartFromLong(range);
        int endSelection = unpackRangeEndFromLong(range);
        if (startSelection >= 0 && endSelection >= 0)
        {
            mLongClickingSelectionStart = startSelection;
            mLongClickingSelectionEnd = endSelection;
            mAssistMgr.drawSelectionHighlight(startSelection, endSelection);
        } else
        {
            mLongClickingSelectionStart = -1;
            mLongClickingSelectionEnd = -1;
        }

        mLongClickingSelectionInitStart = mLongClickingSelectionStart;
        mLongClickingSelectionInitEnd = mLongClickingSelectionEnd;

        return true;
    }


    public boolean onKeyUp(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (mTextActionMode != null)
            {
                Selection.setSelection(mBaseText, getSelectionEnd());
                return true;
            }
        }

        return false;
    }

    public boolean onKeyPreIme(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
        {
            if (mTextActionMode != null)
            {
                Selection.setSelection(mBaseText, getSelectionEnd());
                return true;
            }
        }

        return false;
    }


    public void onDetachedFromWindow()
    {
        mAssistMgr.closeAllHandle();
    }

    public void onFocusChanged(boolean gainFocus)
    {
        //在view第一次显示时，onFocusChanged(true)先调用，
        //然后调用onWindowFocusChanged(true)。此时我们希望
        //只在onWindowFocusChanged()中处理对获得焦点的响应。
        if (!gainFocus && mTargetView.hasWindowFocus())
        {
            mIsTextActionModeStopTemporary = false;
            Selection.setSelection(mBaseText, getSelectionEnd());

            mAssistMgr.closeAllHandle();
            mAssistMgr.eraseSelectionHighlight();
            stopTextActionMode();
        }
    }

    public void onWindowFocusChanged(boolean gainFocus)
    {
        //在view第一次显示时，onFocusChanged(true)先调用，
        //然后调用此方法(true)。在某些情况下，比如屏幕上显示
        //多个fragment，每个fragment中都有此view，那么即使
        //某些fragment设定为不可见(GONE)，这些fragment中的
        //view也会接受到此方法的调用。由于这些view不可见，此时
        //我们不希望此处的代码被执行，因此设立此条件。
        if (gainFocus && mTargetView.hasFocus())
        {
            //在失去焦点时，我们关闭了所有显示的Assistant(除了Selection背景)，
            //那么当再次获得焦点时，理应重新显示滑动之前显示的Assistant(除了两种情况：
            //1.Beyond 2.在失去焦点期间触发其他的代码段使得Assistant被关闭)。那么我们
            //应该在失去焦点时标记已显示的Assistant，并在此处检查标记，显示这些
            //窗口，这需要额外的变量。
            if (hasSelection())
            {
                mAssistMgr.showLeftHandle();
                mAssistMgr.showRightHandle();
            }
        } else if (!gainFocus && mTargetView.hasFocus())
        {
            mAssistMgr.closeAllHandle();
        }
    }

    public void onSizeChanged()
    {
        if (mTargetView.hasFocus())
        {
            if (hasSelection())
            {
                mAssistMgr.showLeftHandle(true);
                mAssistMgr.showRightHandle(true);
                mAssistMgr.drawSelectionHighlight();
            }

            if (mTextActionMode != null && mActionStyle == FLOAT)
                mTextActionMode.invalidateContentRect();
        }
    }


    public void onStartScroll()
    {
        if (mTargetView.hasFocus())
        {
            if (hasSelection())
            {
                mAssistMgr.closeLeftHandle();
                mAssistMgr.closeRightHandle();
            }

            if (mTextActionMode != null)
            {
                mIsTextActionModeStopTemporary = true;
                if (mActionStyle == FLOAT)
                    stopTextActionMode();
            }
        }
    }

    public void onEndScroll()
    {
        if (mTargetView.hasFocus())
        {
            if (hasSelection())
            {
                mAssistMgr.showLeftHandle(true);
                mAssistMgr.showRightHandle(true);
            }

            if (mIsTextActionModeStopTemporary)
            {
                if (mActionStyle == FLOAT)
                    startTextActionMode();
                mIsTextActionModeStopTemporary = false;
            }
        }
    }

    public void onScrolling()
    {
        computeTextOffsetAtViewTop();
    }


    private void startTextActionMode()
    {
        if (mTextActionMode == null)
        {
            mTextActionMode = mTargetView.startActionMode(
                    mTextActionModeCallback,
                    mActionStyle == FLOAT ? ActionMode.TYPE_FLOATING : ActionMode.TYPE_PRIMARY);
        }
    }

    private void stopTextActionMode()
    {
        if (mTextActionMode != null)
            mTextActionMode.finish();
    }


    private void processSelectionChanged(int newStart, int newEnd, int oldStart, int oldEnd)
    {
        Log.d("PricipleConfirmer",
                getClass().getSimpleName() + "(" + mTargetView.mUUID + ")"
                        + " processSelectionChanged(newStart: " + newStart + ", newEnd: " + newEnd + ", oldStart: " + oldStart + ", oldEnd: " + oldEnd + ")");

        //变化前和变化后均为Selection模式
        if (oldStart != oldEnd && newStart != newEnd)
        {
            mAssistMgr.drawSelectionHighlight();
            mAssistMgr.showLeftHandle();
            mAssistMgr.showRightHandle();

            if (mTextActionMode != null)
                mTextActionMode.invalidate();
        }
        //变化前为Selection模式，变化后为普通模式
        else if (oldStart != oldEnd)
        {
            stopTextActionMode();
            mAssistMgr.eraseSelectionHighlight(oldStart, oldEnd);
            mAssistMgr.closeLeftHandle();
            mAssistMgr.closeRightHandle();
        }
        //变化前为普通模式，变化后为Selection模式
        else if (newStart != newEnd)
        {
            stopTextActionMode();

            mAssistMgr.drawSelectionHighlight();
            mAssistMgr.showLeftHandle();
            mAssistMgr.showRightHandle();
            startTextActionMode();
        }
        //变化前和变化后均为普通模式
        else
            stopTextActionMode();

        ((UTextView) mTargetView).scrollToFit();
    }


    private float convertViewXToTextX(float vx)
    {
        vx -= mTargetView.getPaddingLeft();
        vx = Math.max(0f, vx);
        vx = Math.min(mTargetView.getWidth() - mTargetView.getPaddingLeft() - mTargetView.getPaddingRight() - 1, vx);
        vx += mTargetView.getScrollX();

        return vx;
    }

    private float convertViewYToTextY(float vy)
    {
        vy -= mTargetView.getPaddingTop();
        vy = Math.max(0f, vy);
        vy = Math.min(mTargetView.getHeight() - mTargetView.getPaddingTop() - mTargetView.getPaddingBottom() - 1, vy);
        vy += mTargetView.getScrollY();

        return vy;
    }


    public void setMaxWidth(int width)
    {
        mMaxWidth = width;

        if (!mIsAutoWidth)
            makeNewTextLayout(width);
        else
        {
            int textWidth = (int) Math.ceil(Layout.getDesiredWidth(mBaseText, mTextPaint));
            if (textWidth >= width)
                makeNewTextLayout(width);
        }
    }

    public void setText(CharSequence text)
    {
        if (text == null)
            text = new SpannableStringBuilder("");
        mBaseText.replace(0, mBaseText.length(), text);

        mTargetView.requestLayout();
        mTargetView.invalidate();
    }

    public void setTextColor(int color)
    {
        if (mTextDefColor == color) return;

        mTextDefColor = color;
        mTextPaint.setColor(mTextDefColor);

        mTargetView.invalidate();
    }

    public void setTextSize(float size)
    {
        if (mTextDefSize == size) return;

        mTextDefSize = size;
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                mTextDefSize,
                mTargetView.getContext().getResources().getDisplayMetrics()));

        mTargetView.invalidate();
    }

    public void setActionModeStyle(int style)
    {
        if (mActionStyle == style)
            return;

        mActionStyle = style;
        stopTextActionMode();
    }


    /**
     * 设置位于屏幕左上方的文本位置（偏移）。该方法只在初始化时有效，若在调用此方法后又调用了
     * {@link #computeTextOffsetAtViewTop()} 方法，此方法设置的值将被刷新。
     * 默认值为0。
     */
    public void setTextOffsetAtViewTop(int offset)
    {
        mTextOffsetAtViewTop = offset;
    }


    /**
     * 计算此时位于屏幕左上方的文本位置（偏移），并记录此位置。
     * 使用 {@link #getTextOffsetAtViewTop()} 可获得该值。
     * 该方法在 {@link #onScrolling()} 中被调用以实现实时记录。
     */
    public void computeTextOffsetAtViewTop()
    {
        int curLine = mTextLayout.getLineForVertical(mTargetView.getScrollY() - mTargetView.getPaddingTop());
        mTextOffsetAtViewTop = mTextLayout.getOffsetForHorizontal(curLine, 0f);
    }

    /**
     * 获得最近一次记录的位于屏幕左上方的文本位置（偏移）。
     * 使用 {@link #computeTextOffsetAtViewTop()} 可刷新此值。
     * 默认值为0。
     */
    public int getTextOffsetAtViewTop()
    {
        return mTextOffsetAtViewTop;
    }

    public int getScrollOffsetFromTextOffset(int textOffset)
    {
        mTextOffsetAtViewTop = textOffset;

        int finalOffsetY;

        int line = mTextLayout.getLineForOffset(textOffset);
        if (line == 0)
            finalOffsetY = -mTargetView.getScrollY();
        else
        {
            int lineTop = mTextLayout.getLineTop(line);
            finalOffsetY = lineTop + mTargetView.getPaddingTop() - mTargetView.getScrollY();
        }

        return finalOffsetY;
    }

    public int getWidth()
    {
        return mTextLayout.getWidth();
    }

    public int getHeight()
    {
        return mTextLayout.getHeight();
    }

    public CharSequence getText()
    {
        return mBaseText;
    }

    public float getTextSize()
    {
        return mTextDefSize;
    }

    public TextPaint getTextPaint()
    {
        return mTextPaint;
    }

    public DynamicLayout getLayout()
    {
        return mTextLayout;
    }


    public int getSelectionStart()
    {
        int selectionStart = Selection.getSelectionStart(mBaseText);
        int selectionEnd = Selection.getSelectionEnd(mBaseText);

        return Math.min(selectionStart, selectionEnd);
    }

    public int getSelectionEnd()
    {
        int selectionStart = Selection.getSelectionStart(mBaseText);
        int selectionEnd = Selection.getSelectionEnd(mBaseText);

        return Math.max(selectionStart, selectionEnd);
    }

    public String getSelectedText()
    {
        if (!hasSelection())
            return null;

        return String.valueOf(mBaseText.subSequence(getSelectionStart(), getSelectionEnd()));
    }

    public boolean hasSelection()
    {
        return Selection.getSelectionStart(mBaseText)
                != Selection.getSelectionEnd(mBaseText);
    }

    public View getView()
    {
        return mTargetView;
    }

    public UAssistantManager getAssistantManager()
    {
        return mAssistMgr;
    }

    private boolean canCopy()
    {
        return hasSelection();
    }

    private boolean canSelectAll()
    {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();
        if (selectionEnd < selectionStart)
        {
            int temp = selectionEnd;
            selectionEnd = selectionStart;
            selectionStart = temp;
        }

        return !(selectionStart == 0 && selectionEnd == mBaseText.length());
    }

    private boolean canProcessText()
    {
        return hasSelection();
    }

    private boolean canWebSearch()
    {
        return hasSelection();
    }

    private void performCopy()
    {
        int selectionStart = getSelectionStart();
        int selectionEnd = getSelectionEnd();

        mClipMgr.setPrimaryClip(
                ClipData.newPlainText(null,
                        mBaseText.subSequence(selectionStart, selectionEnd)));

        stopTextActionMode();
        Selection.setSelection(mBaseText, getSelectionEnd());
    }

    private void performSelectAll()
    {
        Selection.selectAll(mBaseText);
    }

    private void performWebSearch()
    {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mTargetView.getContext());
        String wsKey = preferences.getString("list_web_searcher", "https://www.baidu.com/s?word=");

        mCCTabsManager.startChromeTab(mTargetView.getContext(), wsKey + getSelectedText());
    }

    private void performTranslate(String translateUrl)
    {
        mCCTabsManager.startChromeTab(mTargetView.getContext(), translateUrl + getSelectedText());
    }


    @SuppressWarnings("unused")
    private class UEditWatcher implements SpanWatcher
    {
        private int mOldSelectionStart = -1;
        private int mOldSelectionEnd = -1;

        @Override
        public void onSpanAdded(Spannable text, Object what, int start, int end)
        {
            Log.d("PricipleConfirmer",
                    getClass().getSimpleName() + "(" + mTargetView.mUUID + ")" +
                            "onSpanAdded("
                            + "text, "
                            + what.getClass().getName() + ", "
                            + String.valueOf(start) + ", "
                            + String.valueOf(end) + ", "
                            + ") called.");

            processSpanChange(text, what, -1, start, -1, end);
        }

        @Override
        public void onSpanRemoved(Spannable text, Object what, int start, int end)
        {
            Log.d("PricipleConfirmer",
                    getClass().getSimpleName() + "(" + mTargetView.mUUID + ")" +
                            "onSpanRemoved("
                            + "text, "
                            + what.getClass().getName() + ", "
                            + String.valueOf(start) + ", "
                            + String.valueOf(end) + ", "
                            + ") called.");

            processSpanChange(text, what, start, -1, end, -1);
        }

        @Override
        public void onSpanChanged(Spannable text, Object what, int ostart, int oend, int nstart, int nend)
        {
            Log.d("PricipleConfirmer",
                    getClass().getSimpleName() + "(" + mTargetView.mUUID + ")" +
                            "onSpanChanged("
                            + "text, "
                            + what.getClass().getName() + ", "
                            + String.valueOf(ostart) + ", "
                            + String.valueOf(oend) + ", "
                            + String.valueOf(nstart) + ", "
                            + String.valueOf(nend) + ", "
                            + ") called.");

            processSpanChange(text, what, ostart, nstart, oend, nend);
        }


        private void processSpanChange(Spanned text, Object what, int oldStart, int newStart, int oldEnd, int newEnd)
        {
            boolean selectionChanged = false;
            int newSelectionStart = -1;
            int newSelectionEnd = -1;

            if (what == Selection.SELECTION_END)
            {
                selectionChanged = true;
                newSelectionEnd = newStart;
            }

            if (what == Selection.SELECTION_START)
            {
                selectionChanged = true;
                newSelectionStart = newStart;
            }

            if (selectionChanged)
            {
                if ((text.getSpanFlags(what) & Spanned.SPAN_INTERMEDIATE) == 0)
                {
                    if (newSelectionStart < 0)
                        newSelectionStart = getSelectionStart();
                    if (newSelectionEnd < 0)
                        newSelectionEnd = getSelectionEnd();

                    processSelectionChanged(newSelectionStart, newSelectionEnd, mOldSelectionStart, mOldSelectionEnd);

                    mOldSelectionStart = newSelectionStart;
                    mOldSelectionEnd = newSelectionEnd;
                }
            }
        }
    }


    private class UTextActionModeCallback extends ActionMode.Callback2
    {
        private RectF mContentBounds = new RectF();

        private boolean updateSelectAllMenuItem(Menu menu)
        {
            boolean canSelectAll = canSelectAll();
            boolean selectAllItemExistes = menu.findItem(MENU_ITEM_ID_SELECT_ALL) != null;
            if (canSelectAll && !selectAllItemExistes)
            {
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
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            mode.setTitle("选择文本");
            mode.setSubtitle(null);
            mode.setTitleOptionalHint(true);

            boolean canCreate = false;

            if (canCopy())
            {
                menu.add(Menu.NONE, MENU_ITEM_ID_COPY, MENU_ITEM_ORDER_COPY, "复制").
                        setAlphabeticShortcut('c').
                        setIcon(R.drawable.ic_action_copy).
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                canCreate = true;
            }

            updateSelectAllMenuItem(menu);

            if (canSelectAll())
                canCreate = true;

            if (canWebSearch())
            {
                menu.add(Menu.NONE, MENU_ITEM_ID_WEB_SEARCH, MENU_ITEM_ORDER_WEB_SEARCH, "网页搜索").
                        setAlphabeticShortcut('s').
                        setIcon(R.drawable.ic_search).
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                canCreate = true;
            }

            if (canProcessText())
            {
                menu.add(Menu.NONE, MENU_ITEM_ID_TRANSLATE, MENU_ITEM_ORDER_TRANSLATE, "翻译").
                        setAlphabeticShortcut('t').
                        setIcon(R.drawable.ic_translate).
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

                canCreate = true;
            }

            return canCreate;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            return updateSelectAllMenuItem(menu);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            switch (item.getItemId())
            {
                case MENU_ITEM_ID_COPY:
                    performCopy();
                    break;

                case MENU_ITEM_ID_SELECT_ALL:
                    performSelectAll();
                    break;

                case MENU_ITEM_ID_WEB_SEARCH:
                    performWebSearch();
                    break;

                case MENU_ITEM_ID_TRANSLATE:
                {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mTargetView.getContext());
                    String wsKey = preferences.getString("list_translator", "http://m.hujiang.com/d/jp/");
                    if (wsKey.equals("GoogleTranslate"))
                    {
                        mTextIntentProcessor.onFetchTextProcessActivity();
                        mTextIntentProcessor.performTextProcess();
                    } else
                    {
                        performTranslate(wsKey);
                    }
                    break;
                }
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            mTextActionMode = null;

            if (hasSelection() && mActionStyle == PRIMARY)
                Selection.setSelection(mBaseText, getSelectionEnd());
        }

        @Override
        public void onGetContentRect(ActionMode mode, View view, Rect outRect)
        {
            mAssistMgr.computeContentRect(mContentBounds);

            // Take TextView's padding and scroll into account.
            int textHorizontalOffset = mTargetView.getPaddingLeft() - mTargetView.getScrollX();
            int textVerticalOffset = mTargetView.getPaddingTop() - mTargetView.getScrollY();
            outRect.set(
                    (int) Math.floor(mContentBounds.left + textHorizontalOffset),
                    (int) Math.floor(mContentBounds.top + textVerticalOffset),
                    (int) Math.ceil(mContentBounds.right + textHorizontalOffset),
                    (int) Math.ceil(mContentBounds.bottom + textVerticalOffset));
        }
    }


    private class TextIntentProcessor
    {
        private PackageManager mPackageManager;
        private ArrayList<HashMap<String, Object>> mTextProcessActivityInfoList;


        public TextIntentProcessor()
        {
            mPackageManager = mTargetView.getContext().getPackageManager();

            mTextProcessActivityInfoList = new ArrayList<>();
        }

        private Intent createProcessTextIntent()
        {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PROCESS_TEXT);
            intent.setType("text/plain");
            return intent;
        }

        private Intent createProcessTextIntentForResolveInfo(ResolveInfo resolveInfo)
        {
            Intent intent = createProcessTextIntent();
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true);
            intent.setClassName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);

            return intent;
        }

        private List<ResolveInfo> getSupportedActivities()
        {
            return mPackageManager.queryIntentActivities(createProcessTextIntent(), 0);
        }

        private CharSequence getLabel(ResolveInfo resolveInfo)
        {
            return resolveInfo.loadLabel(mPackageManager);
        }

        public void onFetchTextProcessActivity()
        {
            mTextProcessActivityInfoList.clear();

            for (ResolveInfo resolveInfo : getSupportedActivities())
            {
                HashMap<String, Object> map = new HashMap<>();

                map.put("TextProcessLabel", getLabel(resolveInfo));
                map.put("TextProcessIntent", createProcessTextIntentForResolveInfo(resolveInfo));

                mTextProcessActivityInfoList.add(map);
            }
        }

        public boolean performTextProcess()
        {
            int size = mTextProcessActivityInfoList.size();
            if (size == 0) return false;

            HashMap<String, Object> map = mTextProcessActivityInfoList.get(0);

            return fireIntent((Intent) map.get("TextProcessIntent"));
        }

        private boolean fireIntent(Intent intent)
        {
            if (intent != null && Intent.ACTION_PROCESS_TEXT.equals(intent.getAction()))
            {
                intent.putExtra(Intent.EXTRA_PROCESS_TEXT, getSelectedText());
                ((Activity) mTargetView.getContext()).startActivityForResult(intent, PROCESS_TEXT_REQUEST_CODE);
                return true;
            }

            return false;
        }
    }


    private class TextIterator
    {
        private BreakIterator mWordIterator;

        public TextIterator()
        {
            mWordIterator = BreakIterator.getWordInstance();
        }

        public long selectWord(int position)
        {
            mWordIterator.setText(mBaseText.toString());
            int end = mWordIterator.following(position);
            int start = mWordIterator.previous();

            return packRangeInLong(start, end);
        }
    }


    private class HandleEventListener implements UAssistantManager.HandleEventListener
    {
        private float mStartLineY;
        private float mStartScreenY;
        private float mEndLineY;
        private float mEndScreenY;

        private final int[] mViewLocationOnScreen = new int[2];


        private boolean determineLeftHandle(float textLayoutX, float textLayoutY)
        {
            int curLine = mTextLayout.getLineForVertical((int) textLayoutY);
            int curOffset = mTextLayout.getOffsetForHorizontal(curLine, textLayoutX);

            int newStartOffset;
            int newEndOffset;
            int prevStartOffset = getSelectionStart();
            int prevEndOffset = getSelectionEnd();

            if (curOffset >= prevEndOffset)
            {
                newStartOffset = prevEndOffset - 1;
                newEndOffset = prevEndOffset;
            } else
            {
                newStartOffset = curOffset;
                newEndOffset = prevEndOffset;
            }

            if (newStartOffset == -1 && newEndOffset == -1)
                return false;

            Selection.setSelection((Spannable) getText(), newStartOffset, newEndOffset);
            mAssistMgr.showLeftHandle();
            return true;
        }


        private boolean determineRightHandle(float textLayoutX, float textLayoutY)
        {
            int curLine = mTextLayout.getLineForVertical((int) textLayoutY);
            int curOffset = mTextLayout.getOffsetForHorizontal(curLine, textLayoutX);

            int newStartOffset;
            int newEndOffset;
            int prevStartOffset = getSelectionStart();
            int prevEndOffset = getSelectionEnd();

            if (curOffset <= prevStartOffset)
            {
                newStartOffset = prevStartOffset;
                newEndOffset = prevStartOffset + 1;
            } else
            {
                newStartOffset = prevStartOffset;
                newEndOffset = curOffset;
            }

            if (newStartOffset == -1 && newEndOffset == -1)
                return false;

            Selection.setSelection((Spannable) getText(), newStartOffset, newEndOffset);
            mAssistMgr.showRightHandle();
            return true;
        }


        @Override
        public void onCapture(UAssistantManager.HandleView view, MotionEvent e)
        {
            int curStartLine = mTextLayout.getLineForOffset(getSelectionStart());
            mStartLineY = (mTextLayout.getLineBottom(curStartLine) + mTextLayout.getLineTop(curStartLine)) / 2f;

            int curEndLine = mTextLayout.getLineForOffset(getSelectionEnd());
            mEndLineY = (mTextLayout.getLineBottom(curEndLine) + mTextLayout.getLineTop(curEndLine)) / 2f;

            mEndScreenY = e.getRawY();
            mStartScreenY = e.getRawY();
        }

        @Override
        public void onDrag(UAssistantManager.HandleView view, MotionEvent e)
        {
            mTargetView.getLocationOnScreen(mViewLocationOnScreen);

            float viewX = e.getRawX() - mViewLocationOnScreen[0];
            float viewY = e.getRawY() - mViewLocationOnScreen[1];
            float textLayoutX = viewX - mTargetView.getPaddingLeft() + mTargetView.getScrollX();
            float textLayoutY = viewY - mTargetView.getPaddingTop() + mTargetView.getScrollY();

            switch (view.getWindowType())
            {
                case LEFT_HANDLE:
                {
                    if (mActionStyle == FLOAT)
                        stopTextActionMode();
                    determineLeftHandle(textLayoutX, mStartLineY + (e.getRawY() - mStartScreenY));
                    break;
                }

                case RIGHT_HANDLE:
                {
                    if (mActionStyle == FLOAT)
                        stopTextActionMode();
                    determineRightHandle(textLayoutX, mEndLineY + (e.getRawY() - mEndScreenY));
                    break;
                }
            }
        }

        @Override
        public void onClick(UAssistantManager.HandleView view, MotionEvent e)
        {
        }

        @Override
        public void onRelease(UAssistantManager.HandleView view)
        {
            switch (view.getWindowType())
            {
                case LEFT_HANDLE:
                    startTextActionMode();
                    break;

                case RIGHT_HANDLE:
                    startTextActionMode();
                    break;
            }
        }
    }


    public interface UTextWatcher
    {
        void onTextChanged(Editable e);
    }
}