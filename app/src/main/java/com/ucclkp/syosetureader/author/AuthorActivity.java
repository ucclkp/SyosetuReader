package com.ucclkp.syosetureader.author;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ucclkp.syosetureader.R;

public class AuthorActivity extends AppCompatActivity
{
    private TabLayout mTabLayout;
    private ActionBar mActionBar;
    private AppBarLayout mAppBarLayout;
    private ViewPager mViewPager;

    private AuthorPagerAdapter mPagerAdapter;


    public final static String ARG_AUTHOR_URL = "arg_author_url";
    public final static String ARG_AUTHOR_NAME = "arg_author_name";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_author);

        Toolbar toolbar = findViewById(R.id.tb_author_activity);
        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        if (mActionBar != null)
            mActionBar.setDisplayHomeAsUpEnabled(true);

        mTabLayout = findViewById(R.id.tl_author_activity);
        mAppBarLayout = findViewById(R.id.abl_author_activity);
        mViewPager = findViewById(R.id.vp_author_activity);

        String authorUrl = "";
        String authorName = "";
        Intent intent = getIntent();
        if (intent != null)
        {
            authorUrl = intent.getStringExtra(ARG_AUTHOR_URL);
            authorName = intent.getStringExtra(ARG_AUTHOR_NAME);
        }

        setTitle(authorName);

        mPagerAdapter = new AuthorPagerAdapter(
                getSupportFragmentManager(), authorUrl);
        mViewPager.setAdapter(mPagerAdapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
