package com.gh4a.activities;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.gh4a.Gh4Application;

import java.io.IOException;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    @Override
    protected void updateComment(long id, String text) throws IOException {
        Gh4Application app = Gh4Application.get(EditPullRequestCommentActivity.this);
        PullRequestService pullService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        CommitComment comment = new CommitComment();
        comment.setBody(text);
        comment.setId(id);
        pullService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }

    @Override
    protected void deleteComment(long id) throws IOException {
        Gh4Application app = Gh4Application.get(EditPullRequestCommentActivity.this);
        PullRequestService pullService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        pullService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), id);
    }
}