package com.gh4a.widget;

import android.graphics.Rect;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.view.View;

public class BottomSheetCompatibleScrollingViewBehavior extends AppBarLayout.ScrollingViewBehavior {

    @Override
    public boolean onRequestChildRectangleOnScreen(CoordinatorLayout parent, View child,
            Rect rectangle, boolean immediate) {
        return true;
    }
}
