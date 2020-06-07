package com.ucclkp.syosetureader.novel;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.ucclkp.syosetureader.MainActivity;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SettingsActivity;
import com.ucclkp.syosetureader.SyosetuCacheManager;
import com.ucclkp.syosetureader.UApplication;


public class NovelActivity extends AppCompatActivity implements
        NovelSectionFragment.OnNovelSectionRecord,
        FormatDialogFragment.OnFormatChangedListener
{
    private int mSavedNightMode;
    private boolean mIsExitByUser;
    private SyosetuCacheManager mCacheManager;

    private ActionBar mActionBar;
    private AppBarLayout mAppBarLayout;

    private NovelFragment mNovelFragment;


    public final static String ARG_NOVEL_URL = "novel_url";

    public final static int FORMAT_RC_SHORT = 1;
    public final static int FORMAT_RC_SECTION = 2;

    final static String FRAGMENT_NOVEL = "fragment_novel";
    final static String FRAGMENT_NOVEL_SECTION = "fragment_novel_section";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel);

        mCacheManager = ((UApplication) getApplication()).getCacheManager();
        mCacheManager.open();

        Toolbar toolbar = findViewById(R.id.tb_novel_activity);
        toolbar.setTitleTextAppearance(this, R.style.ToolbarTitleAppearance);
        toolbar.setSubtitleTextAppearance(this, R.style.ToolbarSubtitleAppearance);
        setSupportActionBar(toolbar);

        mActionBar = getSupportActionBar();
        if (mActionBar != null)
            mActionBar.setDisplayHomeAsUpEnabled(true);

        mSavedNightMode = AppCompatDelegate.getDefaultNightMode();

        mAppBarLayout = findViewById(R.id.abl_novel_activity);

        Intent intent = getIntent();
        if (intent != null)
        {
            String novelUrl = intent.getStringExtra(ARG_NOVEL_URL);
            mNovelFragment = (NovelFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_NOVEL);

            if (mNovelFragment == null)
            {
                mNovelFragment = NovelFragment.newInstance(novelUrl);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fl_novel_replace_content, mNovelFragment, FRAGMENT_NOVEL)
                        .commit();
            }
        }

        setResult(MainActivity.RC_NONE);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.menu_novel_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case android.R.id.home:
            {
                Fragment fragment = getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_NOVEL_SECTION);
                if (fragment != null && fragment.isVisible())
                {
                    getSupportFragmentManager().popBackStackImmediate();
                } else
                {
                    mIsExitByUser = true;
                    mNovelFragment.notifyNovelActivityExit();

                    finish();
                    //Intent upIntent = NavUtils.getParentActivityIntent(this);
                    //NavUtils.navigateUpTo(this, upIntent);
                }
                return true;
            }

            case R.id.menu_novel_activity_action_settings:
                Intent intent = new Intent(
                        NovelActivity.this,
                        SettingsActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        mIsExitByUser = false;

        int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode != mSavedNightMode) {
            recreate();
        }

        Log.d("RetainDBG", "NovelActivity.onStart() invoked.");
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (!mIsExitByUser)
            mNovelFragment.notifyNovelActivityExit();

        mCacheManager.flush();
        Log.d("RetainDBG", "NovelActivity.onStop() invoked.");

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        //mCacheManager.close();
    }

    @Override
    public void onBackPressed()
    {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0)
        {
            mIsExitByUser = true;
            mNovelFragment.notifyNovelActivityExit();
        }
        super.onBackPressed();
    }

    @Override
    public void requestRecord(String sectionUrl, String number, int offset)
    {
        mNovelFragment.recieveNovelRecord(sectionUrl, number, offset);
    }

    @Override
    public void onFontChanged(String fontName, int requestCode)
    {
        switch (requestCode)
        {
            case FORMAT_RC_SHORT:
                mNovelFragment.notifyFontChanged(fontName);
                break;

            case FORMAT_RC_SECTION:
                NovelSectionFragment fragment
                        = (NovelSectionFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_NOVEL_SECTION);
                if (fragment != null && fragment.isVisible())
                    fragment.notifyFontChanged(fontName);
                break;
        }
    }

    @Override
    public void onFontSizeChanged(int size, int requestCode)
    {
        switch (requestCode)
        {
            case FORMAT_RC_SHORT:
                mNovelFragment.notifyFontSizeChanged(size);
                break;

            case FORMAT_RC_SECTION:
                NovelSectionFragment fragment
                        = (NovelSectionFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_NOVEL_SECTION);
                if (fragment != null && fragment.isVisible())
                    fragment.notifyFontSizeChanged(size);
                break;
        }
    }

    @Override
    public void onLineSpacingChanged(String mult, String add, int requestCode)
    {
        switch (requestCode)
        {
            case FORMAT_RC_SHORT:
                mNovelFragment.notifyLineSpacingChanged(mult, add);
                break;

            case FORMAT_RC_SECTION:
                NovelSectionFragment fragment
                        = (NovelSectionFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_NOVEL_SECTION);
                if (fragment != null && fragment.isVisible())
                    fragment.notifyLineSpacingChanged(mult, add);
                break;
        }
    }

    @Override
    public void onBackgroundChanged(Drawable newDrawable, String name, int requestCode)
    {
        switch (requestCode)
        {
            case FORMAT_RC_SHORT:
                mNovelFragment.notifyBackgroundChanged(newDrawable, name);
                break;

            case FORMAT_RC_SECTION:
                NovelSectionFragment fragment
                        = (NovelSectionFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_NOVEL_SECTION);
                if (fragment != null && fragment.isVisible())
                    fragment.notifyBackgroundChanged(newDrawable, name);
                break;
        }
    }
}
