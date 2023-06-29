package com.ucclkp.syosetureader.novel;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import androidx.appcompat.widget.AppCompatSpinner;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.TextView;

import com.ucclkp.syosetureader.PrFloat;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.UApplication;


public class FormatDialogFragment extends BottomSheetDialogFragment
{
    private int mCurFontSize;
    private PrFloat mCurLSA, mCurLSM;
    private String mCurBackgroundId;
    private int mRequestCode;

    private int mAccentColor;

    private AppCompatSpinner mFontSpinner;
    private TextView mFontSizeTextView;
    private ImageButton mFontSizeAddIB, mFontSizeMinusIB;
    private TextView mLSATextView;
    private ImageButton mLSAAddIB, mLSAMinusIB;
    private TextView mLSMTextView;
    private ImageButton mLSMAddIB, mLSMMinusIB;
    private BackgroundBar mBackgroundBar;

    private OnFormatChangedListener mListener;


    public final static int MIN_FONT_SIZE_DIP = 5;
    public final static int MAX_FONT_SIZE_DIP = 60;
    public final static int DEFAULT_FONT_SIZE_DIP = 14;

    public final static String MIN_LSA_DIP = "0.0";
    public final static String MAX_LSA_DIP = "10.0";
    public final static String DEFAULT_LSA_DIP = MIN_LSA_DIP;

    public final static String MIN_LSM = "1.0";
    public final static String MAX_LSM = "2.0";
    public final static String DEFAULT_LSM = "1.2";

    public final static String BACKGROUND_ID_YELLOW = "bg_1";
    public final static String BACKGROUND_ID_BROWN = "bg_2";
    public final static String BACKGROUND_ID_GRAY = "bg_3";
    public final static String BACKGROUND_ID_GREEN = "bg_4";
    public final static String BACKGROUND_ID_DEFAULT = "bg_default";

    public final static String BACKGROUND_ID_NIGHT_BLACK = "bg_5";
    public final static String BACKGROUND_ID_NIGHT_DEFAULT = "bg_night_default";

    public final static String ARG_REQUEST_CODE = "arg_request_code";
    public final static String ARG_CURRENT_FONT_SIZE = "arg_current_font_size";
    public final static String ARG_CURRENT_LSA = "arg_current_lsa";
    public final static String ARG_CURRENT_LSM = "arg_current_lsm";
    public final static String ARG_CURRENT_BACKGROUND_ID = "arg_current_bg_id";


    public static Drawable getBackgroundById(Context context, String id)
    {
        switch (id)
        {
            case BACKGROUND_ID_YELLOW:
                return context.getDrawable(R.color.read_bg_color_yellow);
            case BACKGROUND_ID_BROWN:
                return context.getDrawable(R.color.read_bg_color_brown);
            case BACKGROUND_ID_GRAY:
                return context.getDrawable(R.color.read_bg_color_gray);
            case BACKGROUND_ID_GREEN:
                return context.getDrawable(R.color.read_bg_color_green);

            case BACKGROUND_ID_NIGHT_BLACK:
                return context.getDrawable(R.color.read_bg_color_night_black);
        }

        return null;
    }

    public static boolean isNightBackground(String id)
    {
        if (id.equals(BACKGROUND_ID_NIGHT_BLACK)
                || id.equals(BACKGROUND_ID_NIGHT_DEFAULT))
            return true;
        else
            return false;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
        Dialog dialog = new FormatDialog(getContext(), R.style.FormatBottomDialog);
        dialog.setContentView(R.layout.dialog_format_bottom_sheet);

        TypedArray a = getContext().obtainStyledAttributes(
                new int[]{androidx.appcompat.R.attr.colorAccent});
        mAccentColor = a.getColor(a.getIndex(0), Color.GRAY);
        a.recycle();

        mFontSpinner = (AppCompatSpinner) dialog.findViewById(R.id.sp_fbs_font);
        mFontSpinner.setOnItemSelectedListener(mFontSelectedListener);

        mFontSizeAddIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_add_font_size);
        mFontSizeAddIB.setOnClickListener(mAddFontSizeClickListener);
        mFontSizeMinusIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_minus_font_size);
        mFontSizeMinusIB.setOnClickListener(mMinusFontSizeClickListener);

        mLSAAddIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_add_line_add);
        mLSAAddIB.setOnClickListener(mAddLSAClickListener);
        mLSAMinusIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_minus_line_add);
        mLSAMinusIB.setOnClickListener(mMinusLSAClickListener);

        mLSMAddIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_add_line_mult);
        mLSMAddIB.setOnClickListener(mAddLSMClickListener);
        mLSMMinusIB = (ImageButton) dialog.findViewById(R.id.ib_fbs_minus_line_mult);
        mLSMMinusIB.setOnClickListener(mMinusLSMClickListener);

        mBackgroundBar = (BackgroundBar) dialog.findViewById(R.id.bb_format_background);
        mBackgroundBar.setOnSelectedItemChangedListener(mSelectedBgChangedListener);

        boolean isNightMode = UApplication.isNightMode(getContext());
        if (isNightMode)
        {
            mBackgroundBar.addThumb(BACKGROUND_ID_NIGHT_DEFAULT, true);
            mBackgroundBar.addThumb(BACKGROUND_ID_NIGHT_BLACK);
        } else
        {
            mBackgroundBar.addThumb(BACKGROUND_ID_DEFAULT, true);
            mBackgroundBar.addThumb(BACKGROUND_ID_YELLOW);
            mBackgroundBar.addThumb(BACKGROUND_ID_BROWN);
            mBackgroundBar.addThumb(BACKGROUND_ID_GRAY);
            mBackgroundBar.addThumb(BACKGROUND_ID_GREEN);
        }

        mFontSizeTextView = (TextView) dialog.findViewById(R.id.tv_fbs_fontsize);
        mLSATextView = (TextView) dialog.findViewById(R.id.tv_fbs_line_add);
        mLSMTextView = (TextView) dialog.findViewById(R.id.tv_fbs_line_mult);

        Bundle bundle = getArguments();
        if (bundle != null)
        {
            mRequestCode = bundle.getInt(ARG_REQUEST_CODE);
            mCurFontSize = bundle.getInt(ARG_CURRENT_FONT_SIZE);
            mCurLSA = new PrFloat(bundle.getString(ARG_CURRENT_LSA));
            mCurLSM = new PrFloat(bundle.getString(ARG_CURRENT_LSM));
            mCurBackgroundId = bundle.getString(ARG_CURRENT_BACKGROUND_ID);

            String textSizeStr = mCurFontSize + "sp";
            mFontSizeTextView.setText(textSizeStr);
            mLSATextView.setText(mCurLSA.get());
            mLSMTextView.setText(mCurLSM.get());
            mBackgroundBar.selectThumb(mCurBackgroundId);
        }

        setButtonEnable(mFontSizeAddIB, mCurFontSize < MAX_FONT_SIZE_DIP);
        setButtonEnable(mFontSizeMinusIB, mCurFontSize > MIN_FONT_SIZE_DIP);

        setButtonEnable(mLSAAddIB, mCurLSA.compareTo(MAX_LSA_DIP) < 0);
        setButtonEnable(mLSAMinusIB, mCurLSA.compareTo(MIN_LSA_DIP) > 0);

        setButtonEnable(mLSMAddIB, mCurLSM.compareTo(MAX_LSM) < 0);
        setButtonEnable(mLSMMinusIB, mCurLSM.compareTo(MIN_LSM) > 0);

        return dialog;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        mListener = (OnFormatChangedListener) context;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onStop()
    {
        super.onStop();

        SharedPreferences preferences = getActivity().getSharedPreferences(
                UApplication.PREF_FORMAT, Context.MODE_PRIVATE);
        if (isNightBackground(mCurBackgroundId))
        {
            preferences.edit()
                    .putInt(UApplication.FONT_SIZE, mCurFontSize)
                    .putString(UApplication.LINE_SPACING_ADD, mCurLSA.get())
                    .putString(UApplication.LINE_SPACING_MULT, mCurLSM.get())
                    .putString(UApplication.BACKGROUND_NIGHT_ID, mCurBackgroundId).apply();
        } else
        {
            preferences.edit()
                    .putInt(UApplication.FONT_SIZE, mCurFontSize)
                    .putString(UApplication.LINE_SPACING_ADD, mCurLSA.get())
                    .putString(UApplication.LINE_SPACING_MULT, mCurLSM.get())
                    .putString(UApplication.BACKGROUND_ID, mCurBackgroundId).apply();
        }
    }


    private AdapterView.OnItemSelectedListener mFontSelectedListener
            = new AdapterView.OnItemSelectedListener()
    {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
        {
            String fontName = parent.getSelectedItem().toString();

            if (mListener != null)
                mListener.onFontChanged(fontName, mRequestCode);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent)
        {
        }
    };

    private View.OnClickListener mAddFontSizeClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            ++mCurFontSize;
            String textSizeStr = mCurFontSize + "sp";
            mFontSizeTextView.setText(textSizeStr);

            if (mListener != null)
                mListener.onFontSizeChanged(mCurFontSize, mRequestCode);

            setButtonEnable(mFontSizeAddIB, mCurFontSize < MAX_FONT_SIZE_DIP);
            setButtonEnable(mFontSizeMinusIB, mCurFontSize > MIN_FONT_SIZE_DIP);
        }
    };

    private View.OnClickListener mMinusFontSizeClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            --mCurFontSize;
            String textSizeStr = mCurFontSize + "sp";
            mFontSizeTextView.setText(textSizeStr);

            if (mListener != null)
                mListener.onFontSizeChanged(mCurFontSize, mRequestCode);

            setButtonEnable(mFontSizeMinusIB, mCurFontSize > MIN_FONT_SIZE_DIP);
            setButtonEnable(mFontSizeAddIB, mCurFontSize < MAX_FONT_SIZE_DIP);
        }
    };

    private View.OnClickListener mAddLSAClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCurLSA.inc();
            mLSATextView.setText(mCurLSA.get());

            if (mListener != null)
                mListener.onLineSpacingChanged(mCurLSM.get(), mCurLSA.get(), mRequestCode);

            setButtonEnable(mLSAAddIB, mCurLSA.compareTo(MAX_LSA_DIP) < 0);
            setButtonEnable(mLSAMinusIB, mCurLSA.compareTo(MIN_LSA_DIP) > 0);
        }
    };

    private View.OnClickListener mMinusLSAClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCurLSA.dec();
            mLSATextView.setText(mCurLSA.get());

            if (mListener != null)
                mListener.onLineSpacingChanged(mCurLSM.get(), mCurLSA.get(), mRequestCode);

            setButtonEnable(mLSAMinusIB, mCurLSA.compareTo(MIN_LSA_DIP) > 0);
            setButtonEnable(mLSAAddIB, mCurLSA.compareTo(MAX_LSA_DIP) < 0);
        }
    };

    private View.OnClickListener mAddLSMClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCurLSM.inc();
            mLSMTextView.setText(mCurLSM.get());

            if (mListener != null)
                mListener.onLineSpacingChanged(mCurLSM.get(), mCurLSA.get(), mRequestCode);

            setButtonEnable(mLSMAddIB, mCurLSM.compareTo(MAX_LSM) < 0);
            setButtonEnable(mLSMMinusIB, mCurLSM.compareTo(MIN_LSM) > 0);
        }
    };

    private View.OnClickListener mMinusLSMClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            mCurLSM.dec();
            mLSMTextView.setText(mCurLSM.get());

            if (mListener != null)
                mListener.onLineSpacingChanged(mCurLSM.get(), mCurLSA.get(), mRequestCode);

            setButtonEnable(mLSMMinusIB, mCurLSM.compareTo(MIN_LSM) > 0);
            setButtonEnable(mLSMAddIB, mCurLSM.compareTo(MAX_LSM) < 0);
        }
    };

    private BackgroundBar.OnSelectedItemChangedListener mSelectedBgChangedListener
            = new BackgroundBar.OnSelectedItemChangedListener()
    {
        @Override
        public void onSelectedItemChanged(int position, Drawable drawable, String name)
        {
            mCurBackgroundId = name;

            if (mListener != null)
                mListener.onBackgroundChanged(drawable, name, mRequestCode);
        }
    };


    private void setButtonEnable(ImageButton button, boolean enable)
    {
        if (enable)
        {
            button.setEnabled(true);
            button.getDrawable().setTint(mAccentColor);
        } else
        {
            button.setEnabled(false);
            button.getDrawable().setTint(
                    getContext().getColor(R.color.color_disable_tint));
        }
    }


    public interface OnFormatChangedListener
    {
        void onFontChanged(String fontName, int requestCode);

        void onFontSizeChanged(int size, int requestCode);

        void onLineSpacingChanged(String mult, String add, int requestCode);

        void onBackgroundChanged(Drawable newDrawable, String name, int requestCode);
    }
}