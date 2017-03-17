package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int prNumber, CommitComment comment) {
        Intent intent = new Intent(context, EditPullRequestCommentActivity.class)
                .putExtra("pr", prNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(), comment.getBody());
    }

    @Override
    protected CharSequence getSubtitle() {
        int prNumber = getIntent().getIntExtra("pr", 0);
        return getString(R.string.repo_issue_on, prNumber, mRepoOwner, mRepoName);
    }

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
}