package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;

public class EditCommitCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitSha, CommitComment comment) {
        Intent intent = new Intent(context, EditCommitCommentActivity.class)
                .putExtra("commit", commitSha);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, comment.getId(), 0L, comment.getBody(), 0);
    }

    @Override
    protected void createComment(RepositoryId repoId, CommitComment comment,
            long replyToCommentId) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        String sha = getIntent().getStringExtra("commit");
        commitService.addComment(repoId, sha, comment);
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        CommitService commitService = (CommitService) app.getService(Gh4Application.COMMIT_SERVICE);
        commitService.editComment(repoId, comment);
    }
}