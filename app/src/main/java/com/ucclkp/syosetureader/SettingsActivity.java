package com.ucclkp.syosetureader;


import android.annotation.TargetApi;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.ucclkp.syosetureader.about.AboutActivity;

import java.util.List;

public class SettingsActivity extends AppCompatPreferenceActivity
{
    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context)
    {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setupActionBar();

        GeneralPreferenceFragment fragment = new GeneralPreferenceFragment();
        fragment.setOnPrefClickListener(mPrefClickListener);
        fragment.setOnPrefChangeListener(mPrefChangeListener);

        getFragmentManager().beginTransaction().replace(android.R.id.content,
                fragment).commit();

        setResult(MainActivity.RC_NONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();
        if (id == android.R.id.home)
        {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar()
    {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
        {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target)
    {
        //loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName)
    {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    /**
     * This fragment shows general preferences only. It is used when the
     * activity is showing a two-pane settings UI.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class GeneralPreferenceFragment extends PreferenceFragment
    {
        private Preference.OnPreferenceClickListener mPrefClickListener;
        private Preference.OnPreferenceChangeListener mPrefChangeListener;

        @Override
        public void onCreate(Bundle savedInstanceState)
        {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            PreferenceManager.setDefaultValues(getActivity(), R.xml.pref_general, false);

            Preference pref = findPreference("clear_history");
            pref.setOnPreferenceClickListener(mPrefClickListener);
            pref = findPreference("clear_cache");
            pref.setOnPreferenceClickListener(mPrefClickListener);
            pref = findPreference("clear_login");
            pref.setOnPreferenceClickListener(mPrefClickListener);
            pref = findPreference("other_about");
            pref.setOnPreferenceClickListener(mPrefClickListener);

            UiModeManager uiManager = (UiModeManager) getActivity()
                    .getSystemService(Context.UI_MODE_SERVICE);
            boolean isNightMode = uiManager.getNightMode() == UiModeManager.MODE_NIGHT_YES;

            SwitchPreference switchPref = (SwitchPreference) findPreference("sp_night_mode");
            switchPref.setChecked(isNightMode);
            switchPref.setOnPreferenceChangeListener(mPrefChangeListener);
        }

        public void setOnPrefClickListener(Preference.OnPreferenceClickListener l)
        {
            mPrefClickListener = l;
        }

        public void setOnPrefChangeListener(Preference.OnPreferenceChangeListener l)
        {
            mPrefChangeListener = l;
        }
    }


    private Preference.OnPreferenceClickListener mPrefClickListener
            = new Preference.OnPreferenceClickListener()
    {
        @Override
        public boolean onPreferenceClick(Preference preference)
        {
            String key = preference.getKey();
            switch (key)
            {
                case "clear_history":
                {
                    ((UApplication) getApplication())
                            .getSyosetuLibrary().deleteHis();
                    setResult(MainActivity.RC_REFRESH_HIS);
                    return true;
                }

                case "clear_cache":
                {
                    ((UApplication) getApplication()).getCacheManager().delete();
                    ((UApplication) getApplication()).getCacheManager().open();
                    return true;
                }

                case "clear_login":
                {
                    UApplication.cookieManager.clearAll(SettingsActivity.this);
                    UApplication.cookieManager.addOver18Cookie();
                    return true;
                }

                case "other_about":
                {
                    Intent intent = new Intent(
                            SettingsActivity.this, AboutActivity.class);
                    SettingsActivity.this.startActivity(intent);
                    return true;
                }
            }

            return false;
        }
    };

    private Preference.OnPreferenceChangeListener mPrefChangeListener
            = new Preference.OnPreferenceChangeListener()
    {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue)
        {
            String key = preference.getKey();
            switch (key)
            {
                case "sp_night_mode":
                {
                    //TODO:暂时解决方案。
                    UiModeManager uiManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);

                    if ((boolean) newValue)
                        uiManager.setNightMode(UiModeManager.MODE_NIGHT_YES);
                    else
                        uiManager.setNightMode(UiModeManager.MODE_NIGHT_NO);

                    return true;
                }
            }

            return false;
        }
    };
}
