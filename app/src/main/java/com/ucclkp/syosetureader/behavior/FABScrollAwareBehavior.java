package com.ucclkp.syosetureader.behavior;

import android.animation.Animator;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;

import com.ucclkp.syosetureader.R;

import static android.view.View.SCROLL_AXIS_VERTICAL;

public class FABScrollAwareBehavior extends FloatingActionButton.Behavior {
    public boolean mEnabled = false;
    private static final Interpolator INTERPOLATOR = new FastOutSlowInInterpolator();
    private boolean mIsAnimatingOut = false;

    public FABScrollAwareBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onStartNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout,
            @NonNull FloatingActionButton child, @NonNull View directTargetChild, @NonNull View target,
            @ViewCompat.ScrollAxis int axes, @ViewCompat.NestedScrollType int type) {

        if (mEnabled && target.getId() == R.id.nsv_search_fragment_scroller) {
            // Ensure we react to vertical scrolling
            return axes == SCROLL_AXIS_VERTICAL
                    || super.onStartNestedScroll(
                    coordinatorLayout, child, directTargetChild, target, axes, type);
        }

        return false;
    }

    @Override
    public void onNestedScroll(
            @NonNull CoordinatorLayout coordinatorLayout, @NonNull FloatingActionButton child,
            @NonNull View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, @ViewCompat.NestedScrollType int type) {

        super.onNestedScroll(
                        coordinatorLayout, child,
                        target, dxConsumed, dyConsumed,
                        dxUnconsumed, dyUnconsumed, type);

        if (dyConsumed > 0
                && child.getVisibility() == View.VISIBLE && !mIsAnimatingOut)

            animateOut(child);
        else if (dyConsumed < 0
                && child.getVisibility() != View.VISIBLE)

            animateIn(child);

    }


    // Same animation that FloatingActionButton.Behavior uses to hide the FAB when the AppBarLayout exits
    private void animateOut(final FloatingActionButton button) {
        button.animate().translationY(
                button.getHeight() + getMarginBottom(button)).setInterpolator(INTERPOLATOR).withLayer()
                .setListener(new Animator.AnimatorListener() {
                    public void onAnimationStart(Animator animation) {
                        FABScrollAwareBehavior.this.mIsAnimatingOut = true;
                    }

                    public void onAnimationCancel(Animator animation) {
                        FABScrollAwareBehavior.this.mIsAnimatingOut = false;
                    }

                    public void onAnimationEnd(Animator animation) {
                        FABScrollAwareBehavior.this.mIsAnimatingOut = false;
                        button.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {
                    }
                }).start();
    }

    // Same animation that FloatingActionButton.Behavior uses to show the FAB when the AppBarLayout enters
    private void animateIn(FloatingActionButton button) {
        button.setVisibility(View.VISIBLE);
        button.animate().translationY(0)
                .setInterpolator(INTERPOLATOR).withLayer().setListener(null)
                .start();
    }

    private int getMarginBottom(View v) {
        int marginBottom = 0;
        final ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
            marginBottom = ((ViewGroup.MarginLayoutParams) layoutParams).bottomMargin;
        }
        return marginBottom;
    }
}