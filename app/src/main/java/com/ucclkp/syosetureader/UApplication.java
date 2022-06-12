package com.ucclkp.syosetureader;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AppCompatDelegate;

import com.ucclkp.syosetureader.chromecustomtabs.ChromeCustomTabsManager;


public class UApplication extends Application {
    private SyosetuBooks mSyosetuBooks;
    private SyosetuLibrary mSyosetuLibrary;
    private SyosetuCacheManager mCacheManager;

    public static ImageDownloader imageDownloader;
    public static SyosetuCookieManager cookieManager;
    public static ChromeCustomTabsManager chromeCustomTabsManager;

    public static SyosetuUtility.SyosetuSite syosetuSite;
    public static NovelDownloadService.ControlBridge dlServiceController;


    public final static String PREF_FORMAT = "pref_format";
    public final static String FONT_SIZE = "font_size";
    public final static String LINE_SPACING_ADD = "line_spacing_add";
    public final static String LINE_SPACING_MULT = "line_spacing_mult";
    public final static String BACKGROUND_ID = "background_id";
    public final static String BACKGROUND_NIGHT_ID = "background_night_id";

    public final static String PREF_CONFIG = "pref_config";
    public final static String NOMORE_HINT18 = "nomore_hint18";
    public final static String REMED_OVER18 = "remed_over18";

    public final static String PREF_SYSTEM = "pref_system";
    public final static String NIGHT_MODE = "night_mode";
    public final static String NIGHT_MODE_FS = "night_mode_fs";


    @Override
    public void onCreate() {
        super.onCreate();

        mCacheManager = new SyosetuCacheManager(getApplicationContext());
        mSyosetuBooks = new SyosetuBooks(getApplicationContext());
        mSyosetuLibrary = new SyosetuLibrary(getApplicationContext());

        cookieManager = new SyosetuCookieManager();
        chromeCustomTabsManager = new ChromeCustomTabsManager();
        imageDownloader = new ImageDownloader(mCacheManager);

        syosetuSite = SyosetuUtility.SyosetuSite.NORMAL;
        dlServiceController = null;

        cookieManager.loadCookiesFromLocal(getApplicationContext());
        if (!cookieManager.hasOver18Cookie())
            cookieManager.addOver18Cookie();

        SharedPreferences prefs = getSharedPreferences(PREF_SYSTEM, MODE_PRIVATE);
        boolean nightMode = prefs.getBoolean(NIGHT_MODE, false);
        boolean nightMode_fs = prefs.getBoolean(NIGHT_MODE_FS, true);
        if (nightMode_fs) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        } else {
            if (nightMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }
    }

    public static boolean isNightMode(Context c) {
        boolean isNightMode;
        int night_mode = AppCompatDelegate.getDefaultNightMode();
        if (night_mode == AppCompatDelegate.MODE_NIGHT_YES) {
            isNightMode = true;
        } else if (night_mode == AppCompatDelegate.MODE_NIGHT_NO) {
            isNightMode = false;
        } else {
            int ui_mode = c.getResources().getConfiguration().uiMode &
                    Configuration.UI_MODE_NIGHT_MASK;
            isNightMode = ui_mode == Configuration.UI_MODE_NIGHT_YES;
        }
        return isNightMode;
    }

    public SyosetuBooks getSyosetuBooks() {
        return mSyosetuBooks;
    }

    public SyosetuLibrary getSyosetuLibrary() {
        return mSyosetuLibrary;
    }

    public SyosetuCacheManager getCacheManager() {
        return mCacheManager;
    }
}
