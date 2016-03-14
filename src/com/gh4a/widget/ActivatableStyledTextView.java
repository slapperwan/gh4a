package com.gh4a.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;

// A class mapping the 'activated' state to the 'checked' state for GB compatibility
public class ActivatableStyledTextView extends StyleableTextView implements Checkable {
    private static final int[] CHECKED_STATE_SET = {
        android.R.attr.state_checked
    };

    private boolean mChecked;
    private boolean mActivatable;

    public ActivatableStyledTextView(Context context) {
        super(context);
    }

    public ActivatableStyledTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ActivatableStyledTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setActivatable(boolean activatable) {
        mActivatable = activatable;
        refreshDrawableState();
    }

    @Override
    public boolean isChecked() {
        return mChecked && mActivatable;
    }

    @Override
    public void setChecked(boolean b) {
        mChecked = b;
        refreshDrawableState();
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }
}
