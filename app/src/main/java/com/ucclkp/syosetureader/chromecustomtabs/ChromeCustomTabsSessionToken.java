package com.ucclkp.syosetureader.chromecustomtabs;

import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.util.Log;

/**
 * Wrapper class that can be used as a unique identifier for a session. Also contains an accessor
 * for the {@link ChromeCustomTabsCallback} for the session if there was any.
 */
public class ChromeCustomTabsSessionToken
{
    private static final String TAG = "CustomTabsSessionToken";
    private final ICustomTabsCallback mCallbackBinder;
    private final ChromeCustomTabsCallback mCallback;


    /**
     * {}@hide
     */
    ChromeCustomTabsSessionToken(ICustomTabsCallback callbackBinder)
    {
        mCallbackBinder = callbackBinder;
        mCallback = new ChromeCustomTabsCallback()
        {

            @Override
            public void onNavigationEvent(int navigationEvent, Bundle extras)
            {
                try
                {
                    mCallbackBinder.onNavigationEvent(navigationEvent, extras);
                } catch (RemoteException e)
                {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }
        };
    }

    /**
     * {}@hide
     */
    IBinder getCallbackBinder()
    {
        return mCallbackBinder.asBinder();
    }

    @Override
    public int hashCode()
    {
        return getCallbackBinder().hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof ChromeCustomTabsSessionToken)) return false;
        ChromeCustomTabsSessionToken token = (ChromeCustomTabsSessionToken) o;
        return token.getCallbackBinder().equals(mCallbackBinder.asBinder());
    }

    /**
     * @return {@link ChromeCustomTabsCallback} corresponding to this session if there was any non-null
     * callbacks passed by the client.
     */
    public ChromeCustomTabsCallback getCallback()
    {
        return mCallback;
    }
}