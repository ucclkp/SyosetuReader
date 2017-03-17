package com.ucclkp.syosetureader.chromecustomtabs;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.customtabs.ICustomTabsService;


/**
 * Abstract {@link ServiceConnection} to use while binding to a {@link ChromeCustomTabsService}. Any
 * client implementing this is responsible for handling changes related with the lifetime of the
 * connection like rebinding on disconnect.
 */
public abstract class ChromeCustomTabsServiceConnection implements ServiceConnection
{
    @Override
    public final void onServiceConnected(ComponentName name, IBinder service)
    {
        onCustomTabsServiceConnected(name,
                new ChromeCustomTabsClient(ICustomTabsService.Stub.asInterface(service), name)
                {
                });
    }

    /**
     * Called when a connection to the {@link ChromeCustomTabsService} has been established.
     *
     * @param name   The concrete component name of the service that has been connected.
     * @param client {@link ChromeCustomTabsClient} that contains the {@link IBinder} with which the
     *               connection have been established. All further communication should be initiated
     *               using this client.
     */
    public abstract void onCustomTabsServiceConnected(ComponentName name, ChromeCustomTabsClient client);
}
