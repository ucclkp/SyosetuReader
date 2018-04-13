package com.ucclkp.syosetureader.novelinfo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ucclkp.syosetureader.R;

public class NovelInfoActivity extends AppCompatActivity {
    private ActionBar mActionBar;
    private AppBarLayout mAppBarLayout;


    public final static String ARG_NOVEL_INFO_URL = "arg_novel_info_url";

    private final static String FRAGMENT_NOVEL_INFO = "fragment_novel_info";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_info);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tb_novelinfo_activity);
        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        if (mActionBar != null)
            mActionBar.setDisplayHomeAsUpEnabled(true);

        mAppBarLayout = (AppBarLayout) findViewById(R.id.abl_novelinfo_activity);

        String novelInfoUrl = "";
        Intent intent = getIntent();
        if (intent != null) {
            novelInfoUrl = intent.getStringExtra(ARG_NOVEL_INFO_URL);
        }

        Fragment novelInfoFragment = getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_NOVEL_INFO);
        if (novelInfoFragment == null) {
            novelInfoFragment = NovelInfoFragment.newInstance(novelInfoUrl);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fl_novelinfo_content,
                            novelInfoFragment,
                            FRAGMENT_NOVEL_INFO)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }
}