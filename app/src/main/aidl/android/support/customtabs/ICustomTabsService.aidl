package android.support.customtabs;

import android.net.Uri;
import android.os.Bundle;

import java.util.List;

import android.support.customtabs.ICustomTabsCallback;

interface ICustomTabsService
{
    boolean warmup(long flags) = 1;

    boolean newSession(in ICustomTabsCallback callback) = 2;

    boolean mayLaunchUrl(in ICustomTabsCallback callback, in Uri url, in Bundle extras, in List<Bundle> otherLikelyBundles) = 3;

    Bundle extraCommand(String commandName, in Bundle args) = 4;
}
