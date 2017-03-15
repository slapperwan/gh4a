package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.view.NestedScrollingChild;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

/**
 * A bottom sheet behavior which prevents AppBarLayout from getting collapsed/expanded together with
 * the bottom sheet.
 */
@SuppressWarnings("unused")
public class FixedBottomSheetBehavior extends BottomSheetBehavior<View> {

    private WeakReference<View> mNestedScrollingChildRef;

    public FixedBottomSheetBehavior() {
    }

    public FixedBottomSheetBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
        mNestedScrollingChildRef = new WeakReference<>(findScrollingChild(child));
        return super.onLayoutChild(parent, child, layoutDirection);
    }

    @Override
    public void onNestedPreScroll(CoordinatorLayout coordinatorLayout, View child, View target,
            int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed);

        if (target != mNestedScrollingChildRef.get()) {
            return;
        }

        if (getState() == STATE_COLLAPSED || getState() == STATE_EXPANDED
                && coordinatorLayout.getHeight() - child.getHeight() > 0) {
            consumed[1] = dy;
        }
    }

    // Copied from the super class
    private View findScrollingChild(View view) {
        if (view instanceof NestedScrollingChild) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0, count = group.getChildCount(); i < count; i++) {
                View scrollingChild = findScrollingChild(group.getChildAt(i));
                if (scrollingChild != null) {
                    return scrollingChild;
                }
            }
        }
        return null;
    }
}
