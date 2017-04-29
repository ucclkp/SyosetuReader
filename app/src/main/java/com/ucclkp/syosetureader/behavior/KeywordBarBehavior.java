package com.ucclkp.syosetureader.behavior;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;
import android.view.View;

public class KeywordBarBehavior extends CoordinatorLayout.Behavior<TabLayout>
{
    public KeywordBarBehavior(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public boolean onStartNestedScroll(
            CoordinatorLayout coordinatorLayout, TabLayout child,
            View directTargetChild, View target, int nestedScrollAxes)
    {
        return super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes);
    }
}
