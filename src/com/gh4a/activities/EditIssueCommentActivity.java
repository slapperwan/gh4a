package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;

public class EditIssueCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, Comment comment) {
        return EditCommentActivity.fillInIntent(new Intent(context, EditIssueCommentActivity.class),
                repoOwner, repoName, comment.getId(), comment.getBody());
    }

    @Override
    protected void editComment(RepositoryId repoId, long id, String body) throws IOException {
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        Comment comment = new Comment();
        comment.setId(id);
        comment.setBody(body);
        issueService.editComment(repoId, comment);
    }
}