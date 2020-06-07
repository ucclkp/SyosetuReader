package com.ucclkp.syosetureader.home;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.view.View;

class HomePagerAdapter extends FragmentPagerAdapter
{
    private View mContainer;
    private FragmentManager mFragmentManager;

    private final int PAGE_COUNT = 1;
    public final static int FRAGMENT_PICKUP = 0;


    HomePagerAdapter(Fragment parent, View container)
    {
        super(parent.getFragmentManager());

        mContainer = container;
        mFragmentManager = parent.getFragmentManager();
    }


    public void refreshItem(int position)
    {
        switch (position)
        {
            case FRAGMENT_PICKUP:
            {
                Fragment fragment = mFragmentManager
                        .findFragmentByTag(makeFragmentTag(position));
                OnPageRefreshListener listener = (OnPageRefreshListener) fragment;
                listener.onRefresh();
                break;
            }
        }
    }


    @Override
    public Fragment getItem(int position)
    {
        switch (position)
        {
            case FRAGMENT_PICKUP:
                return PickupSectionFragment.newInstance();
        }

        return null;
    }

    @Override
    public int getCount()
    {
        return PAGE_COUNT;
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public CharSequence getPageTitle(int position)
    {
        switch (position)
        {
            case FRAGMENT_PICKUP:
                return "小説PickUp！";
        }

        return "";
    }


    private String makeFragmentTag(int position)
    {
        return "android:switcher:"
                + mContainer.getId()
                + ":" + getItemId(position);
    }


    interface OnPageRefreshListener
    {
        void onRefresh();
    }
}
