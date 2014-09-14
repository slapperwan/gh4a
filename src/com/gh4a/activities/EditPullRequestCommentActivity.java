package com.gh4a.activities;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

public class EditPullRequestCommentActivity extends EditCommentActivity {
    @Override
    protected void updateComment(long id, String text) {
        new EditCommentTask(id, text).execute();
    }

    @Override
    protected void deleteComment(long id) {
        new DeleteCommentTask(id).execute();
    }

    private class EditCommentTask extends ProgressDialogTask<Void> {
        private long mId;
        private String mBody;

        public EditCommentTask(long id, String body) {
            super(EditPullRequestCommentActivity.this, 0, R.string.saving_msg);
            mId = id;
            mBody = body;
        }

        @Override
        protected Void run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            CommitComment comment = new CommitComment();
            comment.setBody(mBody);
            comment.setId(mId);
            pullService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(EditPullRequestCommentActivity.this, 0, R.string.deleting_msg);
            mId = id;
        }

        @Override
        protected Void run() throws Exception {
            PullRequestService pullService = (PullRequestService)
                    Gh4Application.get(mContext).getService(Gh4Application.PULL_SERVICE);

            pullService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mId);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            setResult(RESULT_OK);
            finish();
        }
    }
}