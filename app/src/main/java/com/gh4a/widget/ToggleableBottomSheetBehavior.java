package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ToggleableBottomSheetBehavior<V extends View> extends BottomSheetBehavior<V> {
    private boolean mEnabled = true;

    public ToggleableBottomSheetBehavior() {}

    public ToggleableBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    @Override
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        return mEnabled && super.onInterceptTouchEvent(parent, child, event);
    }

    @Override
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent event) {
        return mEnabled && super.onTouchEvent(parent, child, event);
    }

    @Override
    public boolean onStartNestedScroll(CoordinatorLayout coordinatorLayout,
            V child, View directTargetChild, View target, int nestedScrollAxes) {
        return mEnabled && super.onStartNestedScroll(coordinatorLayout,
                child, directTargetChild, target, nestedScrollAxes);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout,
            V child, View target, int dx, int dy, int[] consumed) {
        if (mEnabled) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);
        }
    }

    @Override
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
        if (mEnabled) {
            super.onStopNestedScroll(coordinatorLayout, child, target);
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout,
            V child, View target, float velocityX, float velocityY) {
        return mEnabled && super.onNestedPreFling(coordinatorLayout,
                child, target, velocityX, velocityY);
    }
}
