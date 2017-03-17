package com.ucclkp.syosetureader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

public class AgeCertificationDialogFragment extends AppCompatDialogFragment
{
    private int mReqCode;
    private boolean mNomoreHint;
    private OnAgeCertListener mListener;


    public static final String ARG_REQUEST_CODE = "arg_request_code";


    public static AgeCertificationDialogFragment newInstance(int requestCode)
    {
        AgeCertificationDialogFragment fragment
                = new AgeCertificationDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_REQUEST_CODE, requestCode);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mReqCode = getArguments().getInt(ARG_REQUEST_CODE, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.dialog_age_certification, container, false);

        final CheckBox nomoreCheckBox = (CheckBox) parent.findViewById(R.id.cb_age_cert_nomore);

        Button deniedButton = (Button) parent.findViewById(R.id.bt_age_cert_denied);
        deniedButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mListener != null)
                {
                    boolean nomoreHint = nomoreCheckBox.isChecked();
                    if (nomoreHint)
                    {
                        SharedPreferences prefs = getActivity().getSharedPreferences(
                                UApplication.PREF_CONFIG, Context.MODE_PRIVATE);
                        prefs.edit().putBoolean(UApplication.NOMORE_HINT18, true)
                                .putBoolean(UApplication.REMED_OVER18, false).apply();
                    }

                    mListener.onDenied(mReqCode, nomoreHint);
                }

                dismiss();
            }
        });

        Button grantedButton = (Button) parent.findViewById(R.id.bt_age_cert_granted);
        grantedButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (mListener != null)
                {
                    boolean nomoreHint = nomoreCheckBox.isChecked();
                    if (nomoreHint)
                    {
                        SharedPreferences prefs = getActivity().getSharedPreferences(
                                UApplication.PREF_CONFIG, Context.MODE_PRIVATE);
                        prefs.edit().putBoolean(UApplication.NOMORE_HINT18, true)
                                .putBoolean(UApplication.REMED_OVER18, true).apply();
                    }

                    mListener.onGranted(mReqCode, nomoreHint);
                }

                dismiss();
            }
        });

        return parent;
    }


    public void setOnAgeCertListener(OnAgeCertListener l)
    {
        mListener = l;
    }


    public interface OnAgeCertListener
    {
        void onGranted(int reqCode, boolean nomoreHint);

        void onDenied(int reqCode, boolean nomoreHint);
    }
}
