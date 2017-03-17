package com.ucclkp.syosetureader.chromecustomtabs;

import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;

import java.util.List;

/**
 * A class to be used for Custom Tabs related communication. Clients that want to launch Custom Tabs
 * can use this class exclusively to handle all related communication.
 */
public class ChromeCustomTabsSession
{
    private final ICustomTabsService mService;
    private final ICustomTabsCallback mCallback;
    private final ComponentName mComponentName;


    /* package */ ChromeCustomTabsSession(ICustomTabsService service,
                                          ICustomTabsCallback callback,
                                          ComponentName componentName)
    {
        mService = service;
        mCallback = callback;
        mComponentName = componentName;
    }

    /**
     * Tells the browser of a likely future navigation to a URL.
     * The most likely URL has to be specified first. Optionally, a list of
     * other likely URLs can be provided. They are treated as less likely than
     * the first one, and have to be sorted in decreasing priority order. These
     * additional URLs may be ignored.
     * All previous calls to this method will be deprioritized.
     *
     * @param url                Most likely URL.
     * @param extras             Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *                           likelihood order. Inside each Bundle, the client should provide a
     *                           {@link Uri} using {@link ChromeCustomTabsService# KEY_URL} with
     *                           {@link Bundle#putParcelable(String, android.os.Parcelable)}.
     * @return true for success.
     */
    public boolean mayLaunchUrl(Uri url, Bundle extras, List<Bundle> otherLikelyBundles)
    {
        try
        {
            return mService.mayLaunchUrl(mCallback, url, extras, otherLikelyBundles);
        }
        catch (RemoteException e)
        {
            return false;
        }
    }

    /* package */ IBinder getBinder()
    {
        return mCallback.asBinder();
    }

    /* package */ ComponentName getComponentName()
    {
        return mComponentName;
    }
}
