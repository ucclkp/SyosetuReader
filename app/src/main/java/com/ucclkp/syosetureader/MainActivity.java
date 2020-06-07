package com.ucclkp.syosetureader;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ucclkp.syosetureader.download.DownloadFragment;
import com.ucclkp.syosetureader.favorite.FavoriteFragment;
import com.ucclkp.syosetureader.history.HistoryFragment;
import com.ucclkp.syosetureader.home.HomeFragment;
import com.ucclkp.syosetureader.login.LoginActivity;
import com.ucclkp.syosetureader.novel.NovelActivity;
import com.ucclkp.syosetureader.search.SearchFragment;
import com.ucclkp.syosetureader.search.SearchResultFragment;

public class MainActivity extends AppCompatActivity {
    private TabLayout mTabLayout;
    private ActionBar mActionBar;
    private AppBarLayout mAppBarLayout;
    private MenuItem mSearchBarMenuItem;

    private TextView mHeaderTitleTV;
    private USearchView mSearchView;
    private UNormalSearchView mAssistSearchView;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    private FloatingActionButton mSearchFAB;

    private int mNavSelectedId;
    private boolean mIsSubmitCollapsed;
    private int mSavedNightMode;

    private final static int FRAGMENT_HOME = 0;
    private final static int FRAGMENT_SEARCH = 1;
    private final static int FRAGMENT_SEARCH_RESULT = 2;
    private final static int FRAGMENT_HISTORY = 3;
    private final static int FRAGMENT_FAVORITE = 4;
    private final static int FRAGMENT_DOWNLOAD = 5;

    private final static String[] FRAGMENT_TAGS
            = new String[]
            {
                    "fragment_home",
                    "fragment_search",
                    "fragment_search_result",
                    "fragment_history",
                    "fragment_favorite",
                    "fragment_download"
            };

    private final static String BACK_STACK_SEARCH = "back_stack_search";
    private final static String BACK_STACK_BORING = "back_stack_boring";

    public final static int REQ_NOVEL_FROM_HIS = 0x1;
    public final static int REQ_NOVEL_FROM_FAV = 0x2;
    public final static int REQ_SETTINGS = 0x10;
    public final static int RC_NONE = RESULT_FIRST_USER;
    public final static int RC_REFRESH_HIS = 0x2;
    public final static int RC_REFRESH_FAV = 0x4;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.tb_main_activity);
        setSupportActionBar(toolbar);

        mNavSelectedId = -1;
        mIsSubmitCollapsed = false;

        mActionBar = getSupportActionBar();
        mSavedNightMode = AppCompatDelegate.getDefaultNightMode();

        mTabLayout = findViewById(R.id.tl_main_activity);
        mAppBarLayout = findViewById(R.id.abl_main_activity);
        mAssistSearchView = findViewById(R.id.unsv_search_assist);
        mAssistSearchView.setOnQueryTextListener(mAssistQueryTextListener);

        mSearchFAB = findViewById(R.id.fab_search);
        mSearchFAB.setOnClickListener(mSearchFABClickListener);

        mNavigationView = findViewById(R.id.nv_main_activity);
        mNavigationView.setNavigationItemSelectedListener(mNavigationItemSelectedListener);

        mHeaderTitleTV = mNavigationView.getHeaderView(0)
                .findViewById(R.id.tv_nav_draw_header_title);
        mHeaderTitleTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        mDrawerLayout = findViewById(R.id.dl_main_activity);
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar,
                R.string.nav_draw_open, R.string.nav_draw_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                processNavSelect();
                mNavSelectedId = -1;
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mSearchBarMenuItem.collapseActionView();
            }
        };

        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);

        Fragment homeFragment = getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HOME]);
        Fragment searchFragment = getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH]);

        if (searchFragment == null) {
            searchFragment = SearchFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fl_main_replace_content,
                            searchFragment, FRAGMENT_TAGS[FRAGMENT_SEARCH])
                    .hide(searchFragment)
                    .commit();
        }

        if (homeFragment == null) {
            homeFragment = HomeFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fl_main_replace_content,
                            homeFragment, FRAGMENT_TAGS[FRAGMENT_HOME])
                    .commit();
        }

        switch (UApplication.syosetuSite) {
            case NORMAL:
                mNavigationView.setCheckedItem(R.id.drawer_read_novel);
                break;
            case NOCTURNE:
                mNavigationView.setCheckedItem(R.id.drawer_read_novel18);
                break;
        }
    }

    @Nullable
    @Override
    public ActionMode onWindowStartingActionMode(ActionMode.Callback callback) {
        return super.onWindowStartingActionMode(callback);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mActionBarDrawerToggle.syncState();
    }

    @Override
    protected void onStart() {
        super.onStart();

        int nightMode = AppCompatDelegate.getDefaultNightMode();
        if (nightMode != mSavedNightMode) {
            recreate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);

        mSearchBarMenuItem = menu.findItem(R.id.menu_main_action_search);
        mSearchBarMenuItem.setOnActionExpandListener(mSearchViewExpandListener);

        mSearchView = (USearchView) mSearchBarMenuItem.getActionView();
        mSearchView.setOnQueryTextListener(mQueryTextListener);

        SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager()
                .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH]);
        searchFragment.setSearchView(mSearchView);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_main_action_search: {
                Fragment searchFragment = getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH]);

                if (!searchFragment.isVisible()) {
                    getSupportFragmentManager().beginTransaction()
                            .hide(getCurFragment())
                            .show(searchFragment)
                            .addToBackStack(BACK_STACK_SEARCH)
                            .commit();
                }
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        //关闭抽屉
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawers();
            return;
        }

        if (isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_HISTORY])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_FAVORITE])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_DOWNLOAD])) {
            while (true) {
                int count = getSupportFragmentManager()
                        .getBackStackEntryCount();

                if (count > 0) {
                    FragmentManager.BackStackEntry entry =
                            getSupportFragmentManager().getBackStackEntryAt(count - 1);
                    if (entry.getName() != null
                            && entry.getName().equals(BACK_STACK_BORING)) {
                        getSupportFragmentManager().popBackStackImmediate();
                    } else
                        break;
                } else
                    break;
            }

            switch (UApplication.syosetuSite) {
                case NORMAL:
                    mNavigationView.setCheckedItem(R.id.drawer_read_novel);
                    break;
                case NOCTURNE:
                    mNavigationView.setCheckedItem(R.id.drawer_read_novel18);
                    break;
            }

            return;
        }

        super.onBackPressed();

        int count = getSupportFragmentManager()
                .getBackStackEntryCount();
        if (count > 0) {
            FragmentManager.BackStackEntry entry =
                    getSupportFragmentManager().getBackStackEntryAt(count - 1);
            if (entry.getName() != null
                    && entry.getName().equals(BACK_STACK_SEARCH)) {
                getSupportFragmentManager().popBackStackImmediate();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_NOVEL_FROM_HIS:
                if ((resultCode & RC_REFRESH_HIS) == RC_REFRESH_HIS) {
                    HistoryFragment fragment = (HistoryFragment) getSupportFragmentManager()
                            .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HISTORY]);
                    if (fragment != null && fragment.isVisible()) {
                        fragment.refresh();
                    }
                }
                break;

            case REQ_NOVEL_FROM_FAV:
                if ((resultCode & RC_REFRESH_FAV) == RC_REFRESH_FAV) {
                    FavoriteFragment fragment = (FavoriteFragment) getSupportFragmentManager()
                            .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_FAVORITE]);
                    if (fragment != null && fragment.isVisible()) {
                        fragment.refresh();
                    }
                }
                break;

            case REQ_SETTINGS:
                if (resultCode == RC_REFRESH_HIS) {
                    HistoryFragment fragment = (HistoryFragment) getSupportFragmentManager()
                            .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HISTORY]);
                    if (fragment != null && fragment.isVisible()) {
                        fragment.refresh();
                    }
                }
                break;
        }
    }


    private void selectReadNovel() {
        if (UApplication.syosetuSite
                == SyosetuUtility.SyosetuSite.NOCTURNE) {
            UApplication.syosetuSite
                    = SyosetuUtility.SyosetuSite.NORMAL;

            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HOME]);
            homeFragment.refresh();

            SearchResultFragment fragment = (SearchResultFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH_RESULT]);
            if (fragment != null && fragment.isVisible())
                fragment.refresh();
        }

        if (isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_HISTORY])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_FAVORITE])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_DOWNLOAD])) {
            Fragment homeFragment = getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HOME]);

            for (int i = 0; i < getSupportFragmentManager()
                    .getBackStackEntryCount(); ++i)
                getSupportFragmentManager().popBackStack();

            getSupportFragmentManager().beginTransaction()
                    .remove(getCurFragment())
                    .show(homeFragment)
                    .commit();
        }
    }

    private void selectReadNovel18() {
        if (UApplication.syosetuSite
                == SyosetuUtility.SyosetuSite.NORMAL) {
            UApplication.syosetuSite
                    = SyosetuUtility.SyosetuSite.NOCTURNE;

            HomeFragment homeFragment = (HomeFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HOME]);
            homeFragment.refresh();

            SearchResultFragment fragment = (SearchResultFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH_RESULT]);
            if (fragment != null && fragment.isVisible())
                fragment.refresh();
        }

        if (isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_HISTORY])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_FAVORITE])
                || isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_DOWNLOAD])) {
            Fragment homeFragment = getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_HOME]);

            for (int i = 0; i < getSupportFragmentManager()
                    .getBackStackEntryCount(); ++i)
                getSupportFragmentManager().popBackStack();

            getSupportFragmentManager().beginTransaction()
                    .remove(getCurFragment())
                    .show(homeFragment)
                    .commit();
        }
    }

    private void processNavSelect() {
        switch (mNavSelectedId) {
            case R.id.drawer_read_novel: {
                selectReadNovel();
                break;
            }

            case R.id.drawer_read_novel18: {
                selectReadNovel18();
                break;
            }

            case R.id.drawer_history:
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fl_main_replace_content,
                                HistoryFragment.newInstance(),
                                FRAGMENT_TAGS[FRAGMENT_HISTORY])
                        .hide(getCurFragment())
                        .addToBackStack(BACK_STACK_BORING)
                        .commit();
                break;

            case R.id.drawer_favorite:
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fl_main_replace_content,
                                FavoriteFragment.newInstance(),
                                FRAGMENT_TAGS[FRAGMENT_FAVORITE])
                        .hide(getCurFragment())
                        .addToBackStack(BACK_STACK_BORING)
                        .commit();
                break;

            case R.id.drawer_download:
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fl_main_replace_content,
                                DownloadFragment.newInstance(),
                                FRAGMENT_TAGS[FRAGMENT_DOWNLOAD])
                        .hide(getCurFragment())
                        .addToBackStack(BACK_STACK_BORING)
                        .commit();
                break;

            case R.id.drawer_settings: {
                Intent intent = new Intent(
                        MainActivity.this,
                        SettingsActivity.class);
                startActivityForResult(intent, REQ_SETTINGS);
                break;
            }
        }
    }


    private boolean isFragmentVisible(String tag) {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag(tag);
        return (fragment != null) && fragment.isVisible();
    }

    private Fragment getCurFragment() {
        for (String tag : FRAGMENT_TAGS) {
            Fragment fragment = getSupportFragmentManager()
                    .findFragmentByTag(tag);
            if (fragment != null && fragment.isVisible())
                return fragment;
        }

        return null;
    }


    private AgeCertificationDialogFragment.OnAgeCertListener mAgeCertListener
            = new AgeCertificationDialogFragment.OnAgeCertListener() {
        @Override
        public void onGranted(int reqCode, boolean nomoreHint) {
            mNavSelectedId = R.id.drawer_read_novel18;
            mDrawerLayout.closeDrawers();
            mNavigationView.setCheckedItem(R.id.drawer_read_novel18);
        }

        @Override
        public void onDenied(int reqCode, boolean nomoreHint) {
            Toast.makeText(MainActivity.this, "您已选择：非18",
                    Toast.LENGTH_SHORT).show();
        }
    };

    private NavigationView.OnNavigationItemSelectedListener mNavigationItemSelectedListener
            = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            if (item.isChecked()) {
                mDrawerLayout.closeDrawers();
                return false;
            }

            if (item.getItemId() == R.id.drawer_read_novel18) {
                SharedPreferences prefs = getSharedPreferences(
                        UApplication.PREF_CONFIG, MODE_PRIVATE);
                if (prefs.getBoolean(UApplication.NOMORE_HINT18, false)) {
                    if (!prefs.getBoolean(UApplication.REMED_OVER18, false)) {
                        Toast.makeText(MainActivity.this, "您已选择：非18",
                                Toast.LENGTH_SHORT).show();
                        return false;
                    }
                } else {
                    AgeCertificationDialogFragment acDialogFragment
                            = AgeCertificationDialogFragment.newInstance(0);
                    acDialogFragment.setOnAgeCertListener(mAgeCertListener);
                    acDialogFragment.show(getSupportFragmentManager(), "fragment_ac_dialog");

                    return false;
                }
            }

            mNavSelectedId = item.getItemId();
            mDrawerLayout.closeDrawers();
            return true;
        }
    };

    private MenuItem.OnActionExpandListener mSearchViewExpandListener
            = new MenuItem.OnActionExpandListener() {
        @Override
        public boolean onMenuItemActionExpand(MenuItem item) {
            return true;
        }

        @Override
        public boolean onMenuItemActionCollapse(MenuItem item) {
            if (isFragmentVisible(FRAGMENT_TAGS[FRAGMENT_SEARCH]) && !mIsSubmitCollapsed)
                getSupportFragmentManager().popBackStackImmediate();
            mIsSubmitCollapsed = false;
            return true;
        }
    };


    private USearchView.OnQueryTextListener mQueryTextListener
            = new USearchView.OnQueryTextListener() {
        @Override
        public void onQueryTextSubmit(String query) {
            if (query.matches("^n[0-9]{4}[a-zA-Z]{1,2}$")) {
                Intent intent = new Intent(
                        MainActivity.this, NovelActivity.class);
                intent.putExtra(NovelActivity.ARG_NOVEL_URL,
                        SyosetuUtility.getNovelUrl() + "/" + query);
                startActivity(intent);
                return;
            }

            SearchFragment searchFragment = (SearchFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_TAGS[FRAGMENT_SEARCH]);

            String url = searchFragment.getSearchUrl(query);

            getSupportFragmentManager().beginTransaction()
                    .hide(getCurFragment())
                    .add(R.id.fl_main_replace_content,
                            SearchResultFragment.newInstance(query, url),
                            FRAGMENT_TAGS[FRAGMENT_SEARCH_RESULT])
                    .addToBackStack(null)
                    .commit();

            mIsSubmitCollapsed = true;
            mSearchBarMenuItem.collapseActionView();
        }

        @Override
        public void onQueryTextChange(String newText) {
        }
    };

    private UNormalSearchView.OnQueryTextListener mAssistQueryTextListener
            = new UNormalSearchView.OnQueryTextListener() {
        @Override
        public void onQueryTextSubmit(String query) {
            mSearchView.submitQueryText();
        }

        @Override
        public void onQueryTextChange(String newText) {
        }
    };

    private View.OnClickListener mSearchFABClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSearchView.submitQueryText();
        }
    };
}