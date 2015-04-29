package com.gh4a.activities;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    @Override
    protected void editComment(RepositoryId repoId, long id, String body) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullService =
                (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);

        CommitComment comment = new CommitComment();
        comment.setId(id);
        comment.setBody(body);

        pullService.editComment(repoId, comment);
    }

    @Override
    protected void deleteComment(RepositoryId repoId, long id) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullService =
                (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);

        pullService.deleteComment(repoId, id);
    }
}