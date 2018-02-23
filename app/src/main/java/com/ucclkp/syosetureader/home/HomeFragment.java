package com.ucclkp.syosetureader.home;


import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.UApplication;

public class HomeFragment extends Fragment
{
    private Toolbar mToolbar;
    private TabLayout mTabLayout;
    private AppBarLayout mAppBarLayout;
    private ViewPager mHistoryViewPager;
    private HomePagerAdapter mSectionAdapter;


    public HomeFragment()
    {
    }


    public static HomeFragment newInstance()
    {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
        }

        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                getActivity().setTitle(getString(R.string.site_novel));
                break;

            case NOCTURNE:
                getActivity().setTitle(getString(R.string.site_novel18));
                break;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_home, container, false);

        mToolbar = getActivity().findViewById(R.id.tb_main_activity);

        mTabLayout = getActivity().findViewById(R.id.tl_main_activity);
        mTabLayout.setVisibility(View.VISIBLE);

        mAppBarLayout = getActivity().findViewById(R.id.abl_main_activity);

        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        lp.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        mToolbar.setLayoutParams(lp);

        mHistoryViewPager = parent.findViewById(R.id.vp_home);

        mSectionAdapter = new HomePagerAdapter(this, mHistoryViewPager);

        mHistoryViewPager.setAdapter(mSectionAdapter);
        mTabLayout.setupWithViewPager(mHistoryViewPager);

        return parent;
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if (!hidden)
        {
            mTabLayout.setVisibility(View.VISIBLE);
            mAppBarLayout.setExpanded(true, false);

            AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
            lp.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            mToolbar.setLayoutParams(lp);

            switch (UApplication.syosetuSite)
            {
                case NORMAL:
                    getActivity().setTitle(getString(R.string.site_novel));
                    break;

                case NOCTURNE:
                    getActivity().setTitle(getString(R.string.site_novel18));
                    break;
            }
        } else
        {
            if (mTabLayout != null)
            {
                mTabLayout.setVisibility(View.GONE);
                mAppBarLayout.setExpanded(true, false);

                AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
                lp.setScrollFlags(0);
                mToolbar.setLayoutParams(lp);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        outState.putBoolean("recreate", true);
    }

    public void refresh()
    {
        mSectionAdapter.refreshItem(
                HomePagerAdapter.FRAGMENT_PICKUP);

        if (isVisible())
        {
            switch (UApplication.syosetuSite)
            {
                case NORMAL:
                    getActivity().setTitle(getString(R.string.site_novel));
                    break;

                case NOCTURNE:
                    getActivity().setTitle(getString(R.string.site_novel18));
                    break;
            }
        }
    }
}
