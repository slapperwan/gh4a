package com.gh4a.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.os.AsyncTaskCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import java.io.IOException;

public class CommentBoxFragment extends Fragment implements
        View.OnClickListener, TextWatcher, SwipeRefreshLayout.ChildScrollDelegate {
    public interface Callback {
        @StringRes int getCommentEditorHintResId();
        void onSendCommentInBackground(String comment) throws IOException;
        void onCommentSent();
    }

    private View mSendButton;
    private EditText mCommentEditor;
    private Callback mCallback;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (getParentFragment() instanceof Callback) {
            mCallback = (Callback) getParentFragment();
        } else if (activity instanceof Callback) {
            mCallback = (Callback) activity;
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
        mCommentEditor.setHint(mCallback.getCommentEditorHintResId());
        mCommentEditor.addTextChangedListener(this);
    }

    @Override
    public void onClick(View view) {
        Editable comment = mCommentEditor.getText();
        AsyncTaskCompat.executeParallel(new CommentTask(comment.toString()));
        UiUtils.hideImeForView(getActivity().getCurrentFocus());
    }

    @Override
    public boolean canChildScrollUp() {
        return mCommentEditor != null && mCommentEditor.hasFocus()
                && UiUtils.canViewScrollUp(mCommentEditor);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // no-op
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // no-op
    }

    @Override
    public void afterTextChanged(Editable s) {
        mSendButton.setEnabled(!StringUtils.isBlank(s.toString()));
    }

    private class CommentTask extends ProgressDialogTask<Void> {
        private String mText;

        public CommentTask(String text) {
            super(getActivity(), 0, R.string.loading_msg);
            mText = text;
        }

        @Override
        protected Void run() throws IOException {
            mCallback.onSendCommentInBackground(mText);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            ToastUtils.showMessage(mContext, R.string.issue_success_comment);
            mCallback.onCommentSent();

            mCommentEditor.setText(null);
            mCommentEditor.clearFocus();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_comment);
        }
    }
}