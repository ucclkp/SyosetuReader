package com.ucclkp.syosetureader.behavior;

import android.content.Context;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;

public class SearchOffsetBehavior extends AppBarLayout.ScrollingViewBehavior
{
    public SearchOffsetBehavior(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency)
    {
        return dependency instanceof TabLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child,
                                          View dependency)
    {
        int offset = dependency.getBottom() - child.getTop();
        ViewCompat.offsetTopAndBottom(child, offset);

        return false;
    }
}