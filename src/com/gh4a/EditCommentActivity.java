package com.gh4a;

import org.eclipse.egit.github.core.Comment;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.loader.DeleteCommentLoader;
import com.gh4a.loader.EditCommentLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

public class EditCommentActivity extends BaseSherlockFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    private long mCommentId;
    private int mIssueNumber;
    private String mIssueState;
    private String mText;
    private EditText mEditText;
    private ProgressDialog mProgressDialog;

    private LoaderCallbacks<Void> mDeleteCallback = new LoaderCallbacks<Void>() {
        @Override
        public Loader<LoaderResult<Void>> onCreateLoader(int id, Bundle args) {
            return new DeleteCommentLoader(EditCommentActivity.this, mRepoOwner, mRepoName, mCommentId);
        }

        @Override
        public void onResultReady(LoaderResult<Void> result) {
            handleLoadResult(result);
        }
    };

    private LoaderCallbacks<Comment> mEditCallback = new LoaderCallbacks<Comment>() {
        @Override
        public Loader<LoaderResult<Comment>> onCreateLoader(int id, Bundle args) {
            return new EditCommentLoader(EditCommentActivity.this, mRepoOwner,
                    mRepoName, mCommentId, mEditText.getText().toString());
        }

        @Override
        public void onResultReady(LoaderResult<Comment> result) {
            handleLoadResult(result);
        }
    };

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
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        case R.id.accept:
            if (!StringUtils.isBlank(mEditText.getText().toString())) {
                mProgressDialog = showProgressDialog(getResources().getString(R.string.saving_msg), true);
                getSupportLoaderManager().initLoader(1, null, mEditCallback);
                getSupportLoaderManager().getLoader(1).forceLoad();
            }
            return true;
        case R.id.delete:
            AlertDialog.Builder builder = createDialogBuilder();
            builder.setMessage(R.string.delete_comment_message);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    mProgressDialog = showProgressDialog(getResources().getString(R.string.deleting_msg), true);
                    getSupportLoaderManager().initLoader(0, null, mDeleteCallback);
                    getSupportLoaderManager().getLoader(0).forceLoad();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            
            builder.show();
            return true;
        
        default:
            return true;
        }
    }

    private void handleLoadResult(LoaderResult<?> result) {
        stopProgressDialog(mProgressDialog);
        if (isLoaderError(result)) {
            return;
        }

        Intent intent = new Intent().setClass(this, IssueActivity.class);
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

}
