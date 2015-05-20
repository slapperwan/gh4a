/*
* Copyright (C) 2008 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.gh4a.widget;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;

// Copy of framework's ColorDrawable as of API 19, needed for getColor() and setColor()
public class ColorDrawable extends Drawable {
    private ColorState mState;
    private final Paint mPaint = new Paint();
    private boolean mMutated;

    public static ColorDrawable create(int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new ColorDrawableApi21(color);
        }
        return new ColorDrawable(color);
    }

    @TargetApi(21)
    private static class ColorDrawableApi21 extends ColorDrawable {
        protected ColorDrawableApi21(int color) {
            super(color);
        }

        @Override
        public void getOutline(@NonNull Outline outline) {
            outline.setRect(getBounds());
            outline.setAlpha(getAlpha() / 255.0f);
        }
    }

    protected ColorDrawable(int color) {
        this(null);
        setColor(color);
    }

    private ColorDrawable(ColorState state) {
        mState = new ColorState(state);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mState.mChangingConfigurations;
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mState = new ColorState(mState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void draw(Canvas canvas) {
        if ((mState.mUseColor >>> 24) != 0) {
            mPaint.setColor(mState.mUseColor);
            canvas.drawRect(getBounds(), mPaint);
        }
    }

    public int getColor() {
        return mState.mUseColor;
    }

    public void setColor(int color) {
        if (mState.mBaseColor != color || mState.mUseColor != color) {
            mState.mBaseColor = mState.mUseColor = color;
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mState.mUseColor >>> 24;
    }

    @Override
    public void setAlpha(int alpha) {
        alpha += alpha >> 7; // make it 0..256
        int baseAlpha = mState.mBaseColor >>> 24;
        int useAlpha = baseAlpha * alpha >> 8;
        int oldUseColor = mState.mUseColor;
        mState.mUseColor = (mState.mBaseColor << 8 >>> 8) | (useAlpha << 24);
        if (oldUseColor != mState.mUseColor) {
            invalidateSelf();
        }
    }

    @Override
    public void setColorFilter(ColorFilter colorFilter) {
    }

    @Override
    public int getOpacity() {
        switch (mState.mUseColor >>> 24) {
            case 255: return PixelFormat.OPAQUE;
            case 0: return PixelFormat.TRANSPARENT;
        }
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public ConstantState getConstantState() {
        mState.mChangingConfigurations = getChangingConfigurations();
        return mState;
    }

    final static class ColorState extends ConstantState {
        int mBaseColor; // base color, independent of setAlpha()
        int mUseColor; // basecolor modulated by setAlpha()
        int mChangingConfigurations;

        ColorState(ColorState state) {
            if (state != null) {
                mBaseColor = state.mBaseColor;
                mUseColor = state.mUseColor;
                mChangingConfigurations = state.mChangingConfigurations;
            }
        }
        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this);
        }
        @Override
        public Drawable newDrawable(Resources res) {
            return new ColorDrawable(this);
        }
        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }
}
