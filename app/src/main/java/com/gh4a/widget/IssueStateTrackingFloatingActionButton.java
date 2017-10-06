package com.gh4a.widget;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;

import com.gh4a.R;
import com.meisolsson.githubsdk.model.IssueState;

public class IssueStateTrackingFloatingActionButton extends FloatingActionButton {
    private IssueState mState;
    private boolean mMerged;

    private static final int[] STATE_CLOSED = { R.attr.state_closed };
    private static final int[] STATE_MERGED = { R.attr.state_merged };

    public IssueStateTrackingFloatingActionButton(Context context) {
        super(context);
    }

    public IssueStateTrackingFloatingActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IssueStateTrackingFloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setState(IssueState state) {
        mState = state;
        refreshDrawableState();
    }

    public void setMerged(boolean merged) {
        mMerged = merged;
        refreshDrawableState();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        if (mState == IssueState.Closed) {
            mergeDrawableStates(drawableState, STATE_CLOSED);
        }
        if (mMerged) {
            mergeDrawableStates(drawableState, STATE_MERGED);
        }
        return drawableState;
    }
}
