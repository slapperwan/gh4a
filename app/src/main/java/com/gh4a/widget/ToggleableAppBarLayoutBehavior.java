package com.gh4a.widget;

import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.view.MotionEvent;
import android.view.View;

public class ToggleableAppBarLayoutBehavior extends AppBarLayout.Behavior {
    private boolean mEnabled = true;

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout parent, AppBarLayout child,
            View directTargetChild, View target, int nestedScrollAxes) {
        return mEnabled && super.onStartNestedScroll(parent, child, directTargetChild, target,
                nestedScrollAxes);
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent,
            AppBarLayout child, MotionEvent ev) {
        return mEnabled && super.onInterceptTouchEvent(parent, child, ev);
    }
}
