package com.gh4a.widget;

import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.view.MotionEvent;

public class ToggleableAppBarLayoutBehavior extends AppBarLayout.Behavior {
    private boolean mEnabled = true;

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent,
            AppBarLayout child, MotionEvent ev) {
        if (!mEnabled) {
            return false;
        }
        return super.onInterceptTouchEvent(parent, child, ev);
    }
}
