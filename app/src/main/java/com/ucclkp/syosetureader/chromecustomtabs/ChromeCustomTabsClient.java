package com.ucclkp.syosetureader.chromecustomtabs;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.text.TextUtils;

import java.util.List;

/**
 * Class to communicate with a {@link ChromeCustomTabsService} and create
 * {@link ChromeCustomTabsSession} from it.
 */
public class ChromeCustomTabsClient
{
    private final ICustomTabsService mService;
    private final ComponentName mServiceComponentName;

    /** {}@hide */
    ChromeCustomTabsClient(ICustomTabsService service, ComponentName componentName)
    {
        mService = service;
        mServiceComponentName = componentName;
    }

    /**
     * Bind to a {@link ChromeCustomTabsService} using the given package name and
     * {@link ServiceConnection}.
     *
     * @param context     {@link Context} to use while calling
     *                    {@link Context#bindService(Intent, ServiceConnection, int)}
     * @param packageName Package name to set on the {@link Intent} for binding.
     * @param connection  {@link ChromeCustomTabsServiceConnection} to use when binding. This will
     *                    return a {@link ChromeCustomTabsClient} on
     *                    {@link ChromeCustomTabsServiceConnection
     *                    #onCustomTabsServiceConnected(ComponentName, CustomTabsClient)}
     * @return Whether the binding was successful.
     */
    public static boolean bindCustomTabsService(Context context,
                                                String packageName, ChromeCustomTabsServiceConnection connection)
    {
        Intent intent = new Intent(ChromeCustomTabsService.ACTION_CUSTOM_TABS_CONNECTION);
        if (!TextUtils.isEmpty(packageName))
            intent.setPackage(packageName);

        return context.bindService(intent, connection,
                Context.BIND_AUTO_CREATE | Context.BIND_WAIVE_PRIORITY);
    }

    public static void unbindCustomTabsService(Context context,ChromeCustomTabsServiceConnection connection)
    {
        context.unbindService(connection);
    }

    /**
     * Warm up the browser process.
     *
     * @param flags Reserved for future use.
     * @return Whether the warmup was successful.
     */
    public boolean warmup(long flags)
    {
        try
        {
            return mService.warmup(flags);
        }
        catch (RemoteException e)
        {
            return false;
        }
    }

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     *
     * @param callback The callback through which the client will receive updates about the created
     *                 session. Can be null.
     * @return The session object that was created as a result of the transaction. The client can
     * use this to relay {@link ChromeCustomTabsSession#mayLaunchUrl(Uri, Bundle, List)} calls.
     * Null on error.
     */
    public ChromeCustomTabsSession newSession(final ChromeCustomTabsCallback callback)
    {
        ICustomTabsCallback.Stub wrapper = new ICustomTabsCallback.Stub()
        {
            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras)
            {
                if (callback != null) callback.onNavigationEvent(navigationEvent, extras);
            }

            @Override
            public void extraCallback(String callbackName, Bundle args) throws RemoteException
            {
                if (callback != null) callback.extraCallback(callbackName, args);
            }
        };

        try
        {
            if (!mService.newSession(wrapper)) return null;
        }
        catch (RemoteException e)
        {
            return null;
        }
        return new ChromeCustomTabsSession(mService, wrapper, mServiceComponentName);
    }

    public Bundle extraCommand(String commandName, Bundle args)
    {
        try
        {
            return mService.extraCommand(commandName, args);
        }
        catch (RemoteException e)
        {
            return null;
        }
    }
}
