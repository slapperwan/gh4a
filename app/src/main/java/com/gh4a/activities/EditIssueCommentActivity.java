package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;

public class EditIssueCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, int issueNumber, Comment comment,
            @AttrRes int highlightColorAttr) {
        Intent intent = new Intent(context, EditIssueCommentActivity.class)
                .putExtra("issue", issueNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(), 0L, comment.getBody(), highlightColorAttr);
    }

    @Override
    protected void createComment(RepositoryId repoId, CommitComment comment,
            long replyToCommentId) throws IOException {
        int issueNumber = getIntent().getIntExtra("issue", 0);
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
        issueService.createComment(repoId, issueNumber, comment.getBody());
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);
        issueService.editComment(repoId, comment);
    }
}