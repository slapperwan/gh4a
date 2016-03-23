package com.gh4a.fragment;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.gh4a.BaseActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import java.io.IOException;

public class CommentBoxFragment extends Fragment implements
        View.OnClickListener, SwipeRefreshLayout.ChildScrollDelegate,
        AppBarLayout.OnOffsetChangedListener {
    public interface Callback {
        @StringRes int getCommentEditorHintResId();
        void onSendCommentInBackground(String comment) throws IOException;
        void onCommentSent();
    }

    private View mSendButton;
    private EditText mCommentEditor;
    private Callback mCallback;
    private boolean mLocked;

    public void setLocked(boolean locked) {
        mLocked = locked;
        updateLockState();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getParentFragment() instanceof Callback) {
            mCallback = (Callback) getParentFragment();
        } else if (context instanceof Callback) {
            mCallback = (Callback) context;
        } else {
            throw new IllegalStateException("No callback provided");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
            @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.comment_edit_box, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSendButton = view.findViewById(R.id.iv_comment);
        mSendButton.setOnClickListener(this);
        mSendButton.setEnabled(false);

        mCommentEditor = (EditText) view.findViewById(R.id.et_comment);
        mCommentEditor.addTextChangedListener(
                new UiUtils.ButtonEnableTextWatcher(mCommentEditor, mSendButton));

        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).addAppBarOffsetListener(this);
        }
        updateLockState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Activity activity = getActivity();
        if (activity instanceof BaseActivity) {
            ((BaseActivity) activity).removeAppBarOffsetListener(this);
        }
    }

    @Override
    public void onClick(View view) {
        Editable comment = mCommentEditor.getText();
        new CommentTask(comment.toString()).schedule();
        UiUtils.hideImeForView(getActivity().getCurrentFocus());
    }

    @Override
    public void onOffsetChanged(AppBarLayout abl, int verticalOffset) {
        View v = getView();
        if (v != null) {
            int offset = abl.getTotalScrollRange() + verticalOffset;
            if (offset >= 0) {
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(),
                        v.getPaddingRight(), offset);
            }
        }
    }

    @Override
    public boolean canChildScrollUp() {
        return mCommentEditor != null && mCommentEditor.hasFocus()
                && UiUtils.canViewScrollUp(mCommentEditor);
    }

    private void updateLockState() {
        if (mCommentEditor == null) {
            return;
        }

        mCommentEditor.setEnabled(!mLocked);
        mSendButton.setEnabled(!mLocked);

        int hintResId = mLocked
                ? R.string.comment_editor_locked_hint : mCallback.getCommentEditorHintResId();
        mCommentEditor.setHint(hintResId);
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mText;

        public CommentTask(String text) {
            super((BaseActivity) getActivity(), 0, R.string.saving_comment);
            mText = text;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new CommentTask(mText);
        }

        @Override
        protected Void run() throws IOException {
            mCallback.onSendCommentInBackground(mText);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mCallback.onCommentSent();

            mCommentEditor.setText(null);
            mCommentEditor.clearFocus();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.issue_error_comment);
        }
    }
}