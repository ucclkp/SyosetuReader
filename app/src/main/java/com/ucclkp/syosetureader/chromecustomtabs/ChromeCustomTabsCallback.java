package com.ucclkp.syosetureader.chromecustomtabs;


import android.os.Bundle;

/**
 * A callback class for custom tabs client to get messages regarding events in their custom tabs.
 */
public class ChromeCustomTabsCallback
{
    /**
     * Sent when the tab has started loading a page.
     */
    public static final int NAVIGATION_STARTED = 1;

    /**
     * Sent when the tab has finished loading a page.
     */
    public static final int NAVIGATION_FINISHED = 2;

    /**
     * Sent when the tab couldn't finish loading due to a failure.
     */
    public static final int NAVIGATION_FAILED = 3;

    /**
     * Sent when loading was aborted by a user action before it finishes like clicking on a link
     * or refreshing the page.
     */
    public static final int NAVIGATION_ABORTED = 4;

    /**
     * Sent when the tab becomes visible.
     */
    public static final int TAB_SHOWN = 5;

    /**
     * Sent when the tab becomes hidden.
     */
    public static final int TAB_HIDDEN = 6;

    /**
     * To be called when a navigation event happens.
     *
     * @param navigationEvent The code corresponding to the navigation event.
     * @param extras          Reserved for future use.
     */
    public void onNavigationEvent(int navigationEvent, Bundle extras)
    {
    }

    /**
     * Unsupported callbacks that may be provided by the implementation.
     * <p>
     * <p>
     * <strong>Note:</strong>Clients should <strong>never</strong> rely on this callback to be
     * called and/or to have a defined behavior, as it is entirely implementation-defined and not
     * supported.
     * <p>
     * <p> This can be used by implementations to add extra callbacks, for testing or experimental
     * purposes.
     *
     * @param callbackName Name of the extra callback.
     * @param args         Arguments for the calback
     */
    public void extraCallback(String callbackName, Bundle args)
    {
    }
}