package com.gh4a.activities;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;

public class EditCommitCommentActivity extends EditCommentActivity {
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
            super(EditCommitCommentActivity.this, 0, R.string.saving_msg);
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
            setResult(RESULT_OK);
            finish();
        }
    }

    private class DeleteCommentTask extends ProgressDialogTask<Void> {
        private long mId;

        public DeleteCommentTask(long id) {
            super(EditCommitCommentActivity.this, 0, R.string.deleting_msg);
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
            setResult(RESULT_OK);
            finish();
        }
    }
}