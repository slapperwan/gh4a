package com.gh4a.widget;

import android.content.Context;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
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
    public boolean onStartNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child, @NonNull View directTargetChild,
            @NonNull View target, int axes, int type) {
        return mEnabled && super.onStartNestedScroll(coordinatorLayout,
                child, directTargetChild, target, axes, type);
    }

    @Override
    public void onNestedPreScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child, @NonNull View target, int dx, int dy,
            @NonNull int[] consumed, int type) {
        if (mEnabled) {
            super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type);
        }
    }

    @Override
    public void onStopNestedScroll(@NonNull CoordinatorLayout coordinatorLayout,
            @NonNull V child, @NonNull View target, int type) {
        if (mEnabled) {
            super.onStopNestedScroll(coordinatorLayout, child, target, type);
        }
    }

    @Override
    public boolean onNestedPreFling(CoordinatorLayout coordinatorLayout,
            V child, View target, float velocityX, float velocityY) {
        return mEnabled && super.onNestedPreFling(coordinatorLayout,
                child, target, velocityX, velocityY);
    }
}
