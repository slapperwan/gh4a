package com.gh4a.widget;

import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.MotionEvent;
import android.view.View;

public class ToggleableAppBarLayoutBehavior extends AppBarLayout.Behavior {
    private boolean mEnabled = true;

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout parent,
            @NonNull AppBarLayout child, @NonNull View directTargetChild,
            @NonNull View target, int nestedScrollAxes, int type) {
        return mEnabled && super.onStartNestedScroll(parent, child, directTargetChild, target,
                nestedScrollAxes, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull AppBarLayout child, @NonNull View target,
            int dx, int dy, @NonNull int[] consumed, int type) {
        if (mEnabled) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull AppBarLayout child, View target,
            int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed,
            int type, int[] consumed) {
        if (mEnabled) {
            super.onNestedScroll(coordinatorLayout, child, target,
                    dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type, consumed);
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull AppBarLayout child, @NonNull View target, int type) {
        if (mEnabled) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent,
            AppBarLayout child, MotionEvent ev) {
        return mEnabled && super.onInterceptTouchEvent(parent, child, ev);
    }
}
