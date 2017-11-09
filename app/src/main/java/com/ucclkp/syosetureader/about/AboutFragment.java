package com.ucclkp.syosetureader.about;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;

public class AboutFragment extends Fragment
{
    public static AboutFragment newInstance()
    {
        return new AboutFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_about, container, false);

        parent.findViewById(R.id.bt_os_as_about).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fl_content_about_activity, OSLContentFragment.newInstance(((TextView) v).getText().toString()))
                        .addToBackStack(null)
                        .commit();
            }
        });

        parent.findViewById(R.id.bt_os_asl_about).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fl_content_about_activity, OSLContentFragment.newInstance(((TextView) v).getText().toString()))
                        .addToBackStack(null)
                        .commit();
            }
        });

        parent.findViewById(R.id.bt_os_dlc_about).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fl_content_about_activity, OSLContentFragment.newInstance(((TextView) v).getText().toString()))
                        .addToBackStack(null)
                        .commit();
            }
        });

        getActivity().setTitle(getContext().getString(R.string.about));

        return parent;
    }
}
