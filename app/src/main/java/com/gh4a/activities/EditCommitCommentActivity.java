package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.model.request.repository.CreateCommitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;

import java.io.IOException;

public class EditCommitCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitSha, long id, String body) {
        Intent intent = new Intent(context, EditCommitCommentActivity.class)
                .putExtra("commit", commitSha);
        return EditCommentActivity.fillInIntent(intent, repoOwner, repoName, id, 0L, body, 0);
    }

    @Override
    protected void createComment(String repoOwner, String repoName, String body,
            long replyToCommentId) throws IOException {
        RepositoryCommentService service =
                Gh4Application.get().getGitHubService(RepositoryCommentService.class);
        String sha = getIntent().getStringExtra("commit");
        ApiHelpers.throwOnFailure(service.createCommitComment(repoOwner, repoName, sha,
                CreateCommitComment.builder().body(body).build()).blockingGet());
    }

    @Override
    protected void editComment(String repoOwner, String repoName, long commentId,
            String body) throws IOException {
        RepositoryCommentService service =
                Gh4Application.get().getGitHubService(RepositoryCommentService.class);
        ApiHelpers.throwOnFailure(service.editCommitComment(repoOwner, repoName, commentId,
                CommentRequest.builder().body(body).build()).blockingGet());
    }
}