package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int prNumber, long replyToCommentId,
            CommitComment comment, @AttrRes int highlightColorAttr) {
        Intent intent = new Intent(context, EditPullRequestCommentActivity.class)
                .putExtra("pr", prNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(),
                replyToCommentId, comment.getBody(), highlightColorAttr);
    }

    @Override
    protected void createComment(RepositoryId repoId, CommitComment comment,
            long replyToCommentId) throws IOException {
        int prNumber = getIntent().getIntExtra("pr", 0);
        Gh4Application app = Gh4Application.get();
        PullRequestService pullService =
                (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
        if (replyToCommentId != 0L) {
            pullService.replyToComment(repoId, prNumber, (int) replyToCommentId, comment.getBody());
        } else {
            pullService.createComment(repoId, prNumber, comment);
        }
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullService =
                (PullRequestService) app.getService(Gh4Application.PULL_SERVICE);
        pullService.editComment(repoId, comment);
    }
}