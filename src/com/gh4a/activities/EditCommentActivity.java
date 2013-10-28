package com.gh4a.activities;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class EditCommentActivity extends BaseSherlockFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    private long mCommentId;
    private int mIssueNumber;
    private String mIssueState;
    private String mText;
    private EditText mEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.edit_text);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mIssueNumber = data.getInt(Constants.Issue.ISSUE_NUMBER);
        mIssueState = data.getString(Constants.Issue.ISSUE_STATE);
        mCommentId = data.getLong(Constants.Comment.ID);
        mText = data.getString(Constants.Comment.BODY);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getResources().getString(R.string.issue_comment_title) + " " + mCommentId);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        mEditText = (EditText) findViewById(R.id.et_text);
        mEditText.setText(mText);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accept_delete, menu);
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.content_discard_dark);
            menu.getItem(1).setIcon(R.drawable.navigation_accept_dark);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.accept:
            String text = mEditText.getText().toString();
            if (!StringUtils.isBlank(text)) {
                new EditCommentTask(mCommentId, text).execute();
            }
            return true;
        case R.id.delete:
            AlertDialog.Builder builder = createDialogBuilder();
            builder.setMessage(R.string.delete_comment_message);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    new DeleteCommentTask(mCommentId).execute();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            
            builder.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void goToParent() {
        Intent intent = new Intent(this, IssueActivity.class);
        intent.putExtra(Constants.Issue.ISSUE_NUMBER, mIssueNumber);

        if ("com.gh4a.PullRequestActivity".equals(getCallingActivity().getClassName())) {
            intent = new Intent().setClass(this, PullRequestActivity.class);
            intent.putExtra(Constants.PullRequest.NUMBER, mIssueNumber);
        }
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_STATE, mIssueState);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private class EditCommentTask extends ProgressDialogTask<Void> {
        private long mId;
        private String mBody;

        public EditCommentTask(long id, String body) {
            super(EditCommentActivity.this, 0, R.string.saving_msg);
            mId = id;
            mBody = body;
        }

        @Override
        protected Void run() throws Exception {
            IssueService issueService = (IssueService)
                    Gh4Application.get(mContext).getService(Gh4Application.ISSUE_SERVICE);

            Comment comment = new Comment();
            comment.setBody(mBody);
            comment.setId(mId);
            issueService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            goToParent();
        }

        @Override
        protected void onError(Exception e) {
            showError();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(EditCommentActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws Exception {
            IssueService issueService = (IssueService)
                    Gh4Application.get(mContext).getService(Gh4Application.ISSUE_SERVICE);
            
            issueService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            goToParent();
        }

        @Override
        protected void onError(Exception e) {
            showError();
        }
    }
}
