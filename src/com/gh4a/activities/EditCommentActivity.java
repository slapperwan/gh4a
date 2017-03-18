package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.Space;
import android.support.v7.app.ActionBar;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.gh4a.BaseActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.RepositoryId;

import java.io.IOException;

public abstract class EditCommentActivity extends BaseActivity implements View.OnClickListener {
    protected static Intent fillInIntent(Intent baseIntent, String repoOwner, String repoName,
            long id, String body) {
        return baseIntent.putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("id", id)
                .putExtra("body", body);
    }

    protected String mRepoOwner;
    protected String mRepoName;
    private long mCommentId;
    private EditText mEditText;
    private TextWatcher mTextWatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_text);

        CoordinatorLayout rootLayout = getRootLayout();
        FloatingActionButton fab = (FloatingActionButton)
                getLayoutInflater().inflate(R.layout.accept_fab, rootLayout, false);
        fab.setOnClickListener(this);
        rootLayout.addView(fab);

        Space spacer = new Space(this);
        spacer.setMinimumHeight(fab.getHeight() / 2);
        addHeaderView(spacer, false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issue_comment_title) + " " + mCommentId);
        actionBar.setSubtitle(getSubtitle());
        actionBar.setDisplayHomeAsUpEnabled(true);

        mEditText = (EditText) findViewById(R.id.et_text);
        mEditText.setText(getIntent().getStringExtra("body"));

        mTextWatcher = new UiUtils.ButtonEnableTextWatcher(mEditText, fab);
        mEditText.addTextChangedListener(mTextWatcher);

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mCommentId = extras.getLong("id");
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // everything was passed in via intent extras
        return false;
    }

    @Override
    public void onClick(View view) {
        String text = mEditText.getText().toString();
        new EditCommentTask(mCommentId, text).schedule();
    }

    protected abstract CharSequence getSubtitle();
    protected abstract void editComment(RepositoryId repoId, long id, String body) throws IOException;

    private class EditCommentTask extends ProgressDialogTask<Void> {
        private final long mId;
        private final String mBody;

        public EditCommentTask(long id, String body) {
            super(EditCommentActivity.this, R.string.saving_msg);
            mId = id;
            mBody = body;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new EditCommentTask(mId, mBody);
        }

        @Override
        protected Void run() throws Exception {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            editComment(repoId, mId, mBody);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            setResult(RESULT_OK);
            finish();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_edit_comment);
        }
    }
}