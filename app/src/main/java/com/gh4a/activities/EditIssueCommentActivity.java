package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;

public class EditIssueCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, int issueNumber, Comment comment) {
        Intent intent = new Intent(context, EditIssueCommentActivity.class)
                .putExtra("issue", issueNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(), comment.getBody());
    }

    @Override
    protected CharSequence getSubtitle() {
        int issueNumber = getIntent().getIntExtra("issue", 0);
        return getString(R.string.repo_issue_on, issueNumber, mRepoOwner, mRepoName);
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