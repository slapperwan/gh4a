/*
 * Copyright 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.gh4a.R;

/**
 * A layout that draws something in the insets passed to {@link #fitSystemWindows(Rect)}, i.e. the area above UI chrome
 * (status and navigation bars, overlay action bars).
 */
public class ScrimInsetsLinearLayout extends LinearLayout {
    private static final int EDGE_MASK_TOP = 1 << 0;
    private static final int EDGE_MASK_BOTTOM = 1 << 1;
    private static final int EDGE_MASK_LEFT = 1 << 2;
    private static final int EDGE_MASK_RIGHT = 1 << 3;
    private static final int ALL_EDGE_MASK =
            EDGE_MASK_TOP | EDGE_MASK_BOTTOM | EDGE_MASK_LEFT | EDGE_MASK_RIGHT;

    private Drawable mInsetForeground;
    private Rect mInsets;
    private Rect mTempRect = new Rect();
    private OnInsetsCallback mOnInsetsCallback;
    private int mEdgeMask = ALL_EDGE_MASK;

    public ScrimInsetsLinearLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ScrimInsetsLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    @TargetApi(11)
    public ScrimInsetsLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyle) {
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ScrimInsetsView, defStyle, 0);
        if (a == null) {
            return;
        }
        mEdgeMask = a.getInt(R.styleable.ScrimInsetsView_consumedEdges, ALL_EDGE_MASK);
        setInsetForeground(a.getDrawable(R.styleable.ScrimInsetsView_insetForeground));
        a.recycle();

        setWillNotDraw(true);
    }

    public void setInsetForeground(Drawable drawable) {
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(null);
        }
        mInsetForeground = drawable;
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(this);
        }
        invalidate();
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == mInsetForeground;
    }

    @Override
    protected boolean fitSystemWindows(Rect insets) {
        mInsets = new Rect(insets);
        setWillNotDraw(mInsetForeground == null);
        ViewCompat.postInvalidateOnAnimation(this);
        if (mOnInsetsCallback != null) {
            mOnInsetsCallback.onInsetsChanged(this, insets);
        }
        if (mEdgeMask == ALL_EDGE_MASK) {
            return true; // consume all insets
        }

        // consume insets selectively
        if ((mEdgeMask & EDGE_MASK_TOP) != 0) {
            insets.top = 0;
        }
        if ((mEdgeMask & EDGE_MASK_BOTTOM) != 0) {
            insets.bottom = 0;
        }
        if ((mEdgeMask & EDGE_MASK_LEFT) != 0) {
            insets.left = 0;
        }
        if ((mEdgeMask & EDGE_MASK_RIGHT) != 0) {
            insets.right = 0;
        }

        return super.fitSystemWindows(insets);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        int width = getWidth();
        int height = getHeight();
        if (mInsets != null && mInsetForeground != null) {
            int sc = canvas.save();
            canvas.translate(getScrollX(), getScrollY());

            // Top
            mTempRect.set(0, 0, width, mInsets.top);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Bottom
            mTempRect.set(0, height - mInsets.bottom, width, height);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Left
            mTempRect.set(0, mInsets.top, mInsets.left, height - mInsets.bottom);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            // Right
            mTempRect.set(width - mInsets.right, mInsets.top, width, height - mInsets.bottom);
            mInsetForeground.setBounds(mTempRect);
            mInsetForeground.draw(canvas);

            canvas.restoreToCount(sc);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mInsetForeground != null) {
            mInsetForeground.setCallback(null);
        }
    }

    /**
     * Allows the calling container to specify a callback for custom processing when insets change (i.e. when
     * {@link #fitSystemWindows(Rect)} is called. This is useful for setting padding on UI elements based on
     * UI chrome insets (e.g. a Google Map or a ListView). When using with ListView or GridView, remember to set
     * clipToPadding to false.
     */
    public void setOnInsetsCallback(OnInsetsCallback onInsetsCallback) {
        mOnInsetsCallback = onInsetsCallback;
    }

    public interface OnInsetsCallback {
        void onInsetsChanged(ScrimInsetsLinearLayout layout, Rect insets);
    }
}