package com.ucclkp.syosetureader;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public class UNormalSearchView extends LinearLayout
{
    private EditText mQueryEditText;
    private ImageButton mCloseButton;

    private InputMethodManager mIMM;
    private OnQueryTextListener mQueryTextListener;


    public UNormalSearchView(Context context)
    {
        this(context, null);
    }

    public UNormalSearchView(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public UNormalSearchView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        setFocusable(true);
        setFocusableInTouchMode(true);

        mIMM = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.search_assist_layout, this, true);

        mQueryEditText = (EditText) findViewById(R.id.et_exkey_search_assist);
        mQueryEditText.setOnEditorActionListener(mEditorActionListener);
        mQueryEditText.addTextChangedListener(mTextWatcher);

        mCloseButton = (ImageButton) findViewById(R.id.ib_close_search_assist);
        mCloseButton.setOnClickListener(mCloseListener);
    }


    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility)
    {
        super.onVisibilityChanged(changedView, visibility);

        if (visibility == View.GONE || visibility == View.INVISIBLE)
        {
            mQueryEditText.setFocusable(false);
            mQueryEditText.setFocusableInTouchMode(false);
        } else
        {
            mQueryEditText.setFocusable(true);
            mQueryEditText.setFocusableInTouchMode(true);
        }
    }

    @Override
    public boolean hasFocus()
    {
        if (mQueryEditText != null)
            return mQueryEditText.hasFocus();
        else
            return super.hasFocus();
    }


    public void setText(CharSequence text)
    {
        mQueryEditText.setText(text);
        mQueryEditText.setSelection(text.length());
    }

    public void appendText(CharSequence text)
    {
        Editable prevText = mQueryEditText.getText();
        prevText.append(text);
    }

    public void appendTextWithSpace(CharSequence text)
    {
        Editable prevText = mQueryEditText.getText();

        if (TextUtils.getTrimmedLength(prevText) == 0 || prevText.toString().endsWith(" "))
            prevText.append(text);
        else
            prevText.append(" ").append(text);
    }

    public void setOnQueryTextListener(OnQueryTextListener l)
    {
        mQueryTextListener = l;
    }

    public void submitQueryText()
    {
        if (mQueryTextListener != null)
            mQueryTextListener.onQueryTextSubmit(mQueryEditText.getText().toString());
    }

    public CharSequence getText()
    {
        return mQueryEditText.getText();
    }


    private void setImeVisibility(final boolean visible)
    {
        if (visible)
            post(mShowImeRunnable);
        else
        {
            removeCallbacks(mShowImeRunnable);
            mIMM.hideSoftInputFromWindow(getWindowToken(), 0);
        }
    }


    private TextWatcher mTextWatcher = new TextWatcher()
    {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after)
        {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count)
        {
        }

        @Override
        public void afterTextChanged(Editable s)
        {
            String newText = s.toString();
            if (newText.isEmpty())
                mCloseButton.setVisibility(GONE);
            else if (mCloseButton.getVisibility() == GONE)
                mCloseButton.setVisibility(VISIBLE);

            if (mQueryTextListener != null)
                mQueryTextListener.onQueryTextChange(newText);
        }
    };

    private EditText.OnEditorActionListener mEditorActionListener
            = new TextView.OnEditorActionListener()
    {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event)
        {
            if (actionId == EditorInfo.IME_ACTION_SEARCH)
            {
                if (mQueryTextListener != null)
                    mQueryTextListener.onQueryTextSubmit(v.getText().toString());
                return true;
            }

            return false;
        }
    };

    private ImageButton.OnClickListener mCloseListener
            = new OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mQueryEditText.setText("");
        }
    };


    private Runnable mShowImeRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            mIMM.showSoftInput(mQueryEditText, 0);
        }
    };


    public interface OnQueryTextListener
    {
        void onQueryTextSubmit(String query);

        void onQueryTextChange(String newText);
    }
}
