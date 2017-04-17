/*
 * Copyright 2015 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gh4a.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.view.SoundEffectConstants;
import android.widget.Checkable;
import android.widget.ImageButton;

/**
 * An extension to {@link ImageButton} which implements the {@link Checkable} interface.
 */
public class CheckableImageButton extends AppCompatImageButton implements Checkable {
    private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };

    private boolean mIsChecked = false;

    public CheckableImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean isChecked() {
        return mIsChecked;
    }

    public void setChecked(boolean checked) {
        if (mIsChecked != checked) {
            mIsChecked = checked;
            refreshDrawableState();
        }
    }

    public void toggle() {
        setChecked(!mIsChecked);
    }

    @Override // borrowed from CompoundButton#performClick()
    public boolean performClick() {
        toggle();
        final boolean handled = super.performClick();
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        return handled;
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (isChecked()) {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + CHECKED_STATE_SET.length),
                    CHECKED_STATE_SET);
        }
        return super.onCreateDrawableState(extraSpace);
    }
}
