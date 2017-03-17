package com.ucclkp.syosetureader.author;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class AuthorPagerAdapter extends FragmentPagerAdapter
{
    private String mAuthorUrl;


    public final static int FRAGMENT_BASE = 0;
    public final static int FRAGMENT_WORKS = 1;
    public final static int FRAGMENT_ACTIVITIES = 2;
    public final static int FRAGMENT_BOOKMARKS = 3;
    public final static int FRAGMENT_FAV_USER = 4;
    public final static int FRAGMENT_COMMENT = 5;
    public final static int FRAGMENT_REVIEWED = 6;


    AuthorPagerAdapter(FragmentManager fm, String authorUrl)
    {
        super(fm);
        mAuthorUrl = authorUrl;
    }


    @Override
    public Fragment getItem(int position)
    {
        return AuthorInfoFragment.newInstance(
                position, mAuthorUrl);
    }

    @Override
    public int getCount()
    {
        return 2;
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
            case FRAGMENT_BASE:
                return "マイページ";
            case FRAGMENT_WORKS:
                return "作品";
            case FRAGMENT_ACTIVITIES:
                return "活動報告";
            case FRAGMENT_BOOKMARKS:
                return "ブックマーク";
            case FRAGMENT_FAV_USER:
                return "お気に入りユーザ一";
            case FRAGMENT_COMMENT:
                return "評価をつけた作品";
            case FRAGMENT_REVIEWED:
                return "レビューした作品";
        }

        return "";
    }
}
