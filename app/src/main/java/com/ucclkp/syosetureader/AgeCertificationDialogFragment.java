package com.ucclkp.syosetureader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

public class AgeCertificationDialogFragment extends AppCompatDialogFragment {
    public static final String ARG_REQUEST_CODE = "arg_request_code";
    private int mReqCode;
    private OnAgeCertListener mListener;

    public static AgeCertificationDialogFragment newInstance(int requestCode) {
        AgeCertificationDialogFragment fragment
                = new AgeCertificationDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_REQUEST_CODE, requestCode);
        fragment.setArguments(bundle);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mReqCode = getArguments().getInt(ARG_REQUEST_CODE, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.dialog_age_certification, container, false);

        final CheckBox nomoreCheckBox = parent.findViewById(R.id.cb_age_cert_nomore);

        Button deniedButton = parent.findViewById(R.id.bt_age_cert_denied);
        deniedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    boolean nomoreHint = nomoreCheckBox.isChecked();
                    if (nomoreHint) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            SharedPreferences prefs = activity.getSharedPreferences(
                                    UApplication.PREF_CONFIG, Context.MODE_PRIVATE);
                            prefs.edit().putBoolean(UApplication.NOMORE_HINT18, true)
                                    .putBoolean(UApplication.REMED_OVER18, false).apply();
                        }
                    }

                    mListener.onDenied(mReqCode, nomoreHint);
                }

                dismiss();
            }
        });

        Button grantedButton = parent.findViewById(R.id.bt_age_cert_granted);
        grantedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    boolean nomoreHint = nomoreCheckBox.isChecked();
                    if (nomoreHint) {
                        FragmentActivity activity = getActivity();
                        if (activity != null) {
                            SharedPreferences prefs = activity.getSharedPreferences(
                                    UApplication.PREF_CONFIG, Context.MODE_PRIVATE);
                            prefs.edit().putBoolean(UApplication.NOMORE_HINT18, true)
                                    .putBoolean(UApplication.REMED_OVER18, true).apply();
                        }
                    }

                    mListener.onGranted(mReqCode, nomoreHint);
                }

                dismiss();
            }
        });

        return parent;
    }


    public void setOnAgeCertListener(OnAgeCertListener l) {
        mListener = l;
    }


    public interface OnAgeCertListener {
        void onGranted(int reqCode, boolean nomoreHint);

        void onDenied(int reqCode, boolean nomoreHint);
    }
}
