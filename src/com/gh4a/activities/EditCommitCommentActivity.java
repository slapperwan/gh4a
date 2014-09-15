package com.gh4a.activities;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import com.gh4a.Gh4Application;

import java.io.IOException;

public class EditCommitCommentActivity extends EditCommentActivity {
    @Override
    protected void updateComment(long id, String text) throws IOException {
        Gh4Application app = Gh4Application.get(EditCommitCommentActivity.this);
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        Comment comment = new Comment();
        comment.setBody(text);
        comment.setId(id);
        issueService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }

    @Override
    protected void deleteComment(long id) throws IOException {
        Gh4Application app = Gh4Application.get(EditCommitCommentActivity.this);
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        issueService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), id);
    }
}