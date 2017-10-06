package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.io.IOException;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int prNumber, long id, long replyToCommentId, String body,
            @AttrRes int highlightColorAttr) {
        Intent intent = new Intent(context, EditPullRequestCommentActivity.class)
                .putExtra("pr", prNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, id, replyToCommentId, body, highlightColorAttr);
    }

    @Override
    protected void createComment(String repoOwner, String repoName, String body,
            long replyToCommentId) throws IOException {
        int prNumber = getIntent().getIntExtra("pr", 0);
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        CreateReviewComment.Builder requestBuilder = CreateReviewComment.builder().body(body);
        if (replyToCommentId != 0) {
            requestBuilder.inReplyTo(replyToCommentId);
        }
        ApiHelpers.throwOnFailure(service.createReviewComment(
                repoOwner, repoName, prNumber, requestBuilder.build()).blockingGet());
    }

    @Override
    protected void editComment(String repoOwner, String repoName, long commentId,
            String body) throws IOException {
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        ApiHelpers.throwOnFailure(service.editReviewComment(repoOwner, repoName, commentId,
                CommentRequest.builder().body(body).build()).blockingGet());
    }
}