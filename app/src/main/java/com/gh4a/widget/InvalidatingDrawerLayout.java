package com.gh4a.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.util.AttributeSet;

public class InvalidatingDrawerLayout extends DrawerLayout {
    private Drawable mStatusBarBackground;

    public InvalidatingDrawerLayout(Context context) {
        super(context);
    }

    public InvalidatingDrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InvalidatingDrawerLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setStatusBarBackground(Drawable bg) {
        super.setStatusBarBackground(bg);
        updateStatusBarDrawable();
    }

    @Override
    public void setStatusBarBackground(int resId) {
        super.setStatusBarBackground(resId);
        updateStatusBarDrawable();
    }

    @Override
    public void setStatusBarBackgroundColor(int color) {
        super.setStatusBarBackgroundColor(color);
        updateStatusBarDrawable();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || who == mStatusBarBackground;
    }

    private void updateStatusBarDrawable() {
        Drawable newBackground = getStatusBarBackgroundDrawable();
        if (newBackground != mStatusBarBackground) {
            if (mStatusBarBackground != null) {
                mStatusBarBackground.setCallback(null);
            }
            if (newBackground != null) {
                newBackground.setCallback(this);
            }
            mStatusBarBackground = newBackground;
        }
    }
}