package android.support.customtabs;

import android.os.Bundle;

interface ICustomTabsCallback
{
    void onNavigationEvent(int navigationEvent, in Bundle extras) = 1;

    void extraCallback(String callbackName, in Bundle args) = 2;
}