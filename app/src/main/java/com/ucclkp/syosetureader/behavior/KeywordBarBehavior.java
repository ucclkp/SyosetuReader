package com.ucclkp.syosetureader.behavior;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewCompat;
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
