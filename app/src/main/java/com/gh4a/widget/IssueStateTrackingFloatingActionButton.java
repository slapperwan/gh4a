package com.gh4a.widget;

import android.content.Context;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;

import com.gh4a.R;
import com.meisolsson.githubsdk.model.IssueState;

public class IssueStateTrackingFloatingActionButton extends FloatingActionButton {
    private IssueState mState;
    private boolean mMerged;
    private boolean mDraft;

    private static final int[] STATE_CLOSED = { R.attr.state_closed };
    private static final int[] STATE_MERGED = { R.attr.state_merged };
    private static final int[] STATE_DRAFT = { R.attr.state_draft };

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

    public void setDraft(boolean draft) {
        mDraft = draft;
        refreshDrawableState();
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
        if (mMerged) {
            mergeDrawableStates(drawableState, STATE_MERGED);
        } else if (mState == IssueState.Closed) {
            mergeDrawableStates(drawableState, STATE_CLOSED);
        } else if (mDraft) {
            mergeDrawableStates(drawableState, STATE_DRAFT);
        }
        return drawableState;
    }
}
