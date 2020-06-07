package com.ucclkp.syosetureader.about;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class OSLContentFragment extends Fragment
{
    private String mTitle;
    private TextView mContentTextView;

    private static final String ARG_TITLE = "arg_title";


    public static OSLContentFragment newInstance(String title)
    {
        OSLContentFragment fragment = new OSLContentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            mTitle = getArguments().getString(ARG_TITLE);

        getActivity().setTitle(mTitle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_about_content, container, false);

        mContentTextView = (TextView) parent.findViewById(R.id.tv_content_about_fragment);

        if (mTitle.equals(getContext().getString(R.string.about_os_android_sdk)))
        {
            mContentTextView.setText(readLicenseFile("android_sdk_license"));
        } else if (mTitle.equals(getContext().getString(R.string.about_os_android_support_library)))
        {
            mContentTextView.setText(readLicenseFile("android_support_library_license"));
        } else if (mTitle.equals(getContext().getString(R.string.about_os_disklrucache)))
        {
            mContentTextView.setText(readLicenseFile("disklrucache_license"));
        }

        return parent;
    }


    @Nullable
    private String readLicenseFile(String fileName)
    {
        String line;
        String content = "";
        BufferedReader reader = null;

        try
        {
            InputStreamReader isr = new InputStreamReader(
                    getActivity().getAssets().open(fileName), "UTF-8");
            reader = new BufferedReader(isr);

            while ((line = reader.readLine()) != null)
                content += line + "\n";

            return content;
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        } finally
        {
            try
            {
                if (reader != null)
                    reader.close();
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
