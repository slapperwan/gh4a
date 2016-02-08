package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

public class FloatingActionButtonWithVisibilityControl extends FloatingActionButton {
    private boolean mCanShow = true;
    private boolean mShouldBeShown = true;

    public FloatingActionButtonWithVisibilityControl(Context context) {
        super(context);
    }

    public FloatingActionButtonWithVisibilityControl(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingActionButtonWithVisibilityControl(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setCanShow(boolean canShow) {
        if (mCanShow != canShow) {
            mCanShow = canShow;
            if (!mCanShow) {
                hide();
            } else if (mShouldBeShown) {
                show();
            }
        }
    }

    @Override
    public void show() {
        show(null);
    }

    @Override
    public void show(OnVisibilityChangedListener listener) {
        mShouldBeShown = true;
        if (mCanShow) {
            super.show(listener);
        }
    }

    @Override
    public void hide() {
        hide(null);
    }

    @Override
    public void hide(OnVisibilityChangedListener listener) {
        mShouldBeShown = false;
        super.hide(listener);
    }
}
