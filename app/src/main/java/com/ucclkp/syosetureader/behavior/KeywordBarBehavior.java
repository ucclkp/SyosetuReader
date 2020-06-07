package com.ucclkp.syosetureader.behavior;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.google.android.material.tabs.TabLayout;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class KeywordBarBehavior extends CoordinatorLayout.Behavior<TabLayout>
{
    public KeywordBarBehavior(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull TabLayout child, @NonNull View directTargetChild, @NonNull View target,
            @ViewCompat.ScrollAxis int axes, @ViewCompat.NestedScrollType int type)
    {
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, axes, type);
    }
}
