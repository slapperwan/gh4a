package com.gh4a.activities;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.RepositoryId;

import java.io.IOException;

public abstract class EditCommentActivity extends BaseActivity {
    private String mRepoOwner;
    private String mRepoName;
    private long mCommentId;
    private EditText mEditText;
    private TextWatcher mTextWatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_text);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issue_comment_title) + " " + mCommentId);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mEditText = (EditText) findViewById(R.id.et_text);
        mEditText.setText(getIntent().getStringExtra(Constants.Comment.BODY));

        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mCommentId = extras.getLong(Constants.Comment.ID);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // everything was passed in via intent extras
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.accept_delete, menu);

        if (mTextWatcher != null) {
            mEditText.removeTextChangedListener(mTextWatcher);
        }
        mEditText.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(mEditText,
                menu.findItem(R.id.accept)));

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.accept:
                String text = mEditText.getText().toString();
                new EditCommentTask(mCommentId, text).schedule();
                return true;
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setMessage(R.string.delete_comment_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new DeleteCommentTask(mCommentId).schedule();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected abstract void editComment(RepositoryId repoId, long id, String body) throws IOException;
    protected abstract void deleteComment(RepositoryId repoId, long id) throws IOException;

    private class EditCommentTask extends ProgressDialogTask<Void> {
        private long mId;
        private String mBody;

        public EditCommentTask(long id, String body) {
            super(EditCommentActivity.this, 0, R.string.saving_msg);
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

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(EditCommentActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new DeleteCommentTask(mId);
        }

        @Override
        protected Void run() throws Exception {
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            deleteComment(repoId, mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            setResult(RESULT_OK);
            finish();
        }

        @Override
        protected String getErrorMessage() {
            return getContext().getString(R.string.error_delete_comment);
        }
    }
}