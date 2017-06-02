package com.gh4a.widget;

import android.graphics.Rect;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;

public class BottomSheetFixScrollingViewBehavior extends AppBarLayout.ScrollingViewBehavior {
    private boolean mFixEnabled = true;

    public void setFixEnabled(boolean enabled) {
        mFixEnabled = enabled;
    }

    @Override
    public boolean onRequestChildRectangleOnScreen(CoordinatorLayout parent, View child,
            Rect rectangle, boolean immediate) {
        return mFixEnabled ||
                super.onRequestChildRectangleOnScreen(parent, child, rectangle, immediate);
    }
}
