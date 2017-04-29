package com.ucclkp.syosetureader.chromecustomtabs;

import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class ChromeCustomTabsManager
{
    private ChromeCustomTabsClient mChromeCustomTabsClient;

    private int mToolbarColor;

    private Bitmap mActionButtonIcon;
    private Intent mActionButtonPendingIntent;

    private String mActionMenuTitle;
    private Intent mActionMenuPendingIntent;

    private int mExitAnimationRes;
    private int mEnterAnimationRes;

    private final Intent mIntent = new Intent(Intent.ACTION_VIEW);


    /**
     * Don't show any title. Shows only the domain.
     */
    public static final int NO_TITLE = 0;

    /**
     * Shows the page title and the domain.
     */
    public static final int SHOW_PAGE_TITLE = 1;

    //  Must have. Extra used to match the session. Its value is an IBinder passed
    //  whilst creating a news session. See newSession() below. Even if the service is not
    //  used and there is no valid session id to be provided, this extra has to be present
    //  with a null value to launch a custom tab.
    private static final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";


    // Extra that changes the background color for the omnibox. colorInt is an int
    // that specifies a Color.
    private static final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

    /**
     * Boolean extra that enables the url bar to hide as the user scrolls down the page
     */
    public static final String EXTRA_ENABLE_URLBAR_HIDING = "android.support.customtabs.extra.ENABLE_URLBAR_HIDING";

    /**
     * Extra bitmap that specifies the icon of the back button on the toolbar. If the client chooses
     * not to customize it, a default close button will be used.
     */
    public static final String EXTRA_CLOSE_BUTTON_ICON = "android.support.customtabs.extra.CLOSE_BUTTON_ICON";

    /**
     * Extra (int) that specifies state for showing the page title. Default is {@link #NO_TITLE}.
     */
    public static final String EXTRA_TITLE_VISIBILITY_STATE = "android.support.customtabs.extra.TITLE_VISIBILITY";

    // Key that specifies the Bitmap to be used as the image source for the
    // action button.
    private static final String KEY_CUSTOM_TABS_ICON = "android.support.customtabs.customaction.ICON";

    /**
     * Key that specifies the content description for the custom action button.
     */
    public static final String KEY_DESCRIPTION =
            "android.support.customtabs.customaction.DESCRIPTION";


    // Key that specifies the PendingIntent to launch when the action button
    // or menu item was tapped. Chrome will be calling PendingIntent#send() on
    // taps after adding the url as data. The client app can call
    // Intent#getDataString() to get the url.
    public static final String KEY_CUSTOM_TABS_PENDING_INTENT = "android.support.customtabs.customaction.PENDING_INTENT";

    /**
     * Extra boolean that specifies whether the custom action button should be tinted. Default is
     * false and the action button will not be tinted.
     */
    public static final String EXTRA_TINT_ACTION_BUTTON =
            "android.support.customtabs.extra.TINT_ACTION_BUTTON";


    // Optional. Use a bundle for parameters if an the action button is specified.
    public static final String EXTRA_CUSTOM_TABS_ACTION_BUTTON_BUNDLE = "android.support.customtabs.extra.ACTION_BUTTON_BUNDLE";


    // Key for the title string for a given custom menu item
    public static final String KEY_CUSTOM_TABS_MENU_TITLE = "android.support.customtabs.customaction.MENU_ITEM_TITLE";


    // Optional. Use an ArrayList for specifying menu related params. There
    // should be a separate Bundle for each custom menu item.
    public static final String EXTRA_CUSTOM_TABS_MENU_ITEMS = "android.support.customtabs.extra.MENU_ITEMS";


    // Optional. Bundle constructed out of
    // ActivityOptions that Chrome will be running when
    // it finishes CustomTabActivity. If you start the Custom Tab with
    // a customized animation, you can specify a matching animation when Custom Tab
    // returns to your app.
    public static final String EXTRA_CUSTOM_TABS_EXIT_ANIMATION_BUNDLE = "android.support.customtabs.extra.EXIT_ANIMATION_BUNDLE";


    // Package name for the Chrome channel the client wants to connect to. This
    // depends on the channel name.
    // Stable = com.android.chrome
    // Beta = com.chrome.beta
    // Dev = com.chrome.dev
    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";


    public ChromeCustomTabsManager()
    {
        mChromeCustomTabsClient = null;
    }


    private void configureToolbarColor(Context context)
    {
        TypedValue typedValue = new TypedValue();
        TypedArray a = context.obtainStyledAttributes(typedValue.data, new int[]{android.R.attr.colorPrimary});
        mToolbarColor = a.getColor(0, Color.BLACK);
        a.recycle();

        mIntent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, mToolbarColor);
    }

    private void configureCustomActionButton()
    {
        Bundle actionButtonBundle = new Bundle();
        actionButtonBundle.putParcelable(KEY_CUSTOM_TABS_ICON, mActionButtonIcon);
        actionButtonBundle.putParcelable(KEY_CUSTOM_TABS_PENDING_INTENT, mActionButtonPendingIntent);
        mIntent.putExtra(EXTRA_CUSTOM_TABS_ACTION_BUTTON_BUNDLE, actionButtonBundle);
    }

    private void configureCustomMenu()
    {
        ArrayList<Bundle> menuItemBundleList = new ArrayList<>();
        // For each menu item do:
        Bundle menuItem = new Bundle();
        menuItem.putString(KEY_CUSTOM_TABS_MENU_TITLE, mActionMenuTitle);
        menuItem.putParcelable(KEY_CUSTOM_TABS_PENDING_INTENT, mActionMenuPendingIntent);
        menuItemBundleList.add(menuItem);
        mIntent.putParcelableArrayListExtra(EXTRA_CUSTOM_TABS_MENU_ITEMS, menuItemBundleList);
    }

    private Bundle configureCustomAnimations(Context context)
    {
        Bundle finishBundle =
                ActivityOptions.makeCustomAnimation(context,
                        mEnterAnimationRes,
                        mExitAnimationRes).toBundle();

        mIntent.putExtra(EXTRA_CUSTOM_TABS_EXIT_ANIMATION_BUNDLE, finishBundle);

        return ActivityOptions.makeCustomAnimation(context,
                mEnterAnimationRes,
                mExitAnimationRes).toBundle();
    }

    private void configureChromeSession(Context context)
    {
        ChromeCustomTabsSession session = null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        if (mChromeCustomTabsClient != null)
        {
            /*boolean warmup = preferences.getBoolean("warmup_chrome", false);
            if(warmup)
            {
                if (!mChromeCustomTabsClient.warmup(0))
                    Log.e("XError", getClass().getName() + ": Warming up failed.");
            }*/

            session = mChromeCustomTabsClient.newSession(new ChromeCustomTabsCallback());
            if (session == null)
                Log.e("XError", getClass().getName() + ": Creating Chrome session failed.");
        }

        Bundle bundle = new Bundle();
        if (session != null)
            mIntent.setPackage(session.getComponentName().getPackageName());
        safePutBinder(bundle, EXTRA_CUSTOM_TABS_SESSION, session == null ? null : session.getBinder());
        mIntent.putExtras(bundle);

        boolean preload = preferences.getBoolean("preload_chrome_website", false);
        if (preload && session != null)
        {
            if (!session.mayLaunchUrl(Uri.parse("https://www.baidu.com"), null, null))
                Log.e("XError", getClass().getName() + ": Preload url failed in Chrome.");
        }
    }


    public void startChromeTab(Context context, String url)
    {
        mIntent.setData(Uri.parse(url));

        if (mChromeCustomTabsClient == null)
            configureChromeSession(context);

        configureToolbarColor(context);
        //configureCustomActionButton();
        //configureCustomMenu();
        //animationBundle = configureCustomAnimations();

        context.startActivity(mIntent);
    }

    public void connectChromeService(Context context)
    {
        if (mChromeCustomTabsClient != null) return;

        boolean succeed = ChromeCustomTabsClient.bindCustomTabsService(context,
                CUSTOM_TAB_PACKAGE_NAME,
                mChromeCustomTabsServiceConnection);

        if (!succeed)
            Log.e("XError", getClass().getName() + ": Binding Chrome service failed.");
    }

    public void disconnectChromeService(Context context)
    {
        if (mChromeCustomTabsClient != null)
            ChromeCustomTabsClient.unbindCustomTabsService(context, mChromeCustomTabsServiceConnection);
    }

    public void setToolbarColor(int color)
    {
        mToolbarColor = color;
    }


    /**
     * A convenience method to handle putting an {@link IBinder} inside a {@link Bundle} for all
     * Android version.
     *
     * @param bundle The bundle to insert the {@link IBinder}.
     * @param key    The key to use while putting the {@link IBinder}.
     * @param binder The {@link IBinder} to put.
     * @return Whether the operation was successful.
     */
    private boolean safePutBinder(Bundle bundle, String key, IBinder binder)
    {
        try
        {
            // {@link Bundle#putBinder} exists since JB MR2, but we have
            // {@link Bundle#putIBinder} which is the same method since the dawn of time. Use
            // reflection when necessary to call it.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
            {
                bundle.putBinder(key, binder);
            } else
            {
                Method putBinderMethod =
                        Bundle.class.getMethod("putIBinder", String.class, IBinder.class);
                putBinderMethod.invoke(bundle, key, binder);
            }
        } catch (InvocationTargetException | IllegalAccessException
                | IllegalArgumentException | NoSuchMethodException e)
        {
            return false;
        }
        return true;
    }


    private ChromeCustomTabsServiceConnection mChromeCustomTabsServiceConnection = new ChromeCustomTabsServiceConnection()
    {
        @Override
        public void onCustomTabsServiceConnected(ComponentName name, ChromeCustomTabsClient client)
        {
            mChromeCustomTabsClient = client;

            /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean warmup = preferences.getBoolean("warmup_chrome", false);

            if (warmup)
            {
                if (!mChromeCustomTabsClient.warmup(0))
                    Log.e("XError", getClass().getName() + ": Warmup Chrome failed.");
            }

            configureChromeSession();*/
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mChromeCustomTabsClient = null;
        }
    };
}