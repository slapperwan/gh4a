package com.gh4a.activities;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;

public class EditIssueCommentActivity extends EditCommentActivity {
    @Override
    protected void editComment(RepositoryId repoId, long id, String body) throws IOException {
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        Comment comment = new Comment();
        comment.setId(id);
        comment.setBody(body);
        issueService.editComment(repoId, comment);
    }

    @Override
    protected void deleteComment(RepositoryId repoId, long id) throws IOException {
        Gh4Application app = Gh4Application.get();
        IssueService issueService = (IssueService) app.getService(Gh4Application.ISSUE_SERVICE);

        issueService.deleteComment(repoId, id);
    }
}