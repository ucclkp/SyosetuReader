package com.ucclkp.syosetureader.chromecustomtabs;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.support.customtabs.ICustomTabsService;
import android.util.ArrayMap;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * Abstract service class for implementing Custom Tabs related functionality. The service should
 * be responding to the action ACTION_CUSTOM_TABS_CONNECTION. This class should be used by
 * implementers that want to provide Custom Tabs functionality, not by clients that want to launch
 * Custom Tabs.
 */
public abstract class ChromeCustomTabsService extends Service
{
    private final Map<IBinder, IBinder.DeathRecipient> mDeathRecipientMap = new ArrayMap<>();

    /**
     * The Intent action that a ChromeCustomTabsService must respond to.
     */
    public static final String ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService";


    private ICustomTabsService.Stub mBinder = new ICustomTabsService.Stub()
    {
        @Override
        public boolean warmup(long flags) throws RemoteException
        {
            return false;
        }

        @Override
        public boolean newSession(ICustomTabsCallback callback) throws RemoteException
        {
            return false;
        }

        @Override
        public boolean mayLaunchUrl(ICustomTabsCallback callback, Uri url, Bundle extras, List<Bundle> otherLikelyBundles) throws RemoteException
        {
            return false;
        }

        @Override
        public Bundle extraCommand(String commandName, Bundle args) throws RemoteException
        {
            return null;
        }
    };


    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }


    /**
     * Called when the client side {@link IBinder} for this {@link ChromeCustomTabsSessionToken} is dead.
     * Can also be used to clean up {@link IBinder.DeathRecipient} instances allocated for the given token.
     *
     * @param sessionToken The session token for which the {@link IBinder.DeathRecipient} call has been
     *                     received.
     * @return Whether the clean up was successful. Multiple calls with two tokens holdings the
     * same binder will return false.
     */
    protected boolean cleanUpSession(ChromeCustomTabsSessionToken sessionToken)
    {
        try
        {
            synchronized (mDeathRecipientMap)
            {
                IBinder binder = sessionToken.getCallbackBinder();
                IBinder.DeathRecipient deathRecipient = mDeathRecipientMap.get(binder);
                binder.unlinkToDeath(deathRecipient, 0);
                mDeathRecipientMap.remove(binder);
            }
        }
        catch (NoSuchElementException e)
        {
            return false;
        }
        return true;
    }

    /**
     * Warms up the browser process asynchronously.
     *
     * @param flags Reserved for future use.
     * @return Whether warmup was/had been completed successfully. Multiple successful
     * calls will return true.
     */
    protected abstract boolean warmup(long flags);

    /**
     * Creates a new session through an ICustomTabsService with the optional callback. This session
     * can be used to associate any related communication through the service with an intent and
     * then later with a Custom Tab. The client can then send later service calls or intents to
     * through same session-intent-Custom Tab association.
     *
     * @param sessionToken Session token to be used as a unique identifier. This also has access
     *                     to the {@link ChromeCustomTabsCallback} passed from the client side through
     *                     {@link ChromeCustomTabsSessionToken#getCallback()}.
     * @return Whether a new session was successfully created.
     */
    protected abstract boolean newSession(ChromeCustomTabsSessionToken sessionToken);

    /**
     * Tells the browser of a likely future navigation to a URL.
     * <p>
     * The method {@link ChromeCustomTabsService#warmup(long)} has to be called beforehand.
     * The most likely URL has to be specified explicitly. Optionally, a list of
     * other likely URLs can be provided. They are treated as less likely than
     * the first one, and have to be sorted in decreasing priority order. These
     * additional URLs may be ignored.
     * All previous calls to this method will be deprioritized.
     *
     * @param sessionToken       The unique identifier for the session. Can not be null.
     * @param url                Most likely URL.
     * @param extras             Reserved for future use.
     * @param otherLikelyBundles Other likely destinations, sorted in decreasing
     *                           likelihood order. Each Bundle has to provide a url.
     * @return Whether the call was successful.
     */
    protected abstract boolean mayLaunchUrl(ChromeCustomTabsSessionToken sessionToken, Uri url,
                                            Bundle extras, List<Bundle> otherLikelyBundles);

    /**
     * Unsupported commands that may be provided by the implementation.
     * <p>
     * <p>
     * <strong>Note:</strong>Clients should <strong>never</strong> rely on this method to have a
     * defined behavior, as it is entirely implementation-defined and not supported.
     * <p>
     * <p> This call can be used by implementations to add extra commands, for testing or
     * experimental purposes.
     *
     * @param commandName Name of the extra command to execute.
     * @param args        Arguments for the command
     * @return The result {@link Bundle}, or null.
     */
    protected abstract Bundle extraCommand(String commandName, Bundle args);
}