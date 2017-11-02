package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import io.reactivex.Single;

public class EditPullRequestCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            int prNumber, long id, long replyToCommentId, String body,
            @AttrRes int highlightColorAttr) {
        // This activity only supports editing or replying to comments,
        // not creating new unrelated ones.
        if (id == 0L && replyToCommentId == 0L) {
            throw new IllegalStateException("Only editing and replying allowed");
        }
        Intent intent = new Intent(context, EditPullRequestCommentActivity.class)
                .putExtra("pr", prNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, id, replyToCommentId, body, highlightColorAttr);
    }

    @Override
    protected Single<GitHubCommentBase> createComment(String repoOwner, String repoName,
            String body, long replyToCommentId) {
        int prNumber = getIntent().getIntExtra("pr", 0);
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        CreateReviewComment request = CreateReviewComment.builder()
                .body(body)
                .inReplyTo(replyToCommentId)
                .build();
        return service.createReviewComment(repoOwner, repoName, prNumber, request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    protected Single<GitHubCommentBase> editComment(String repoOwner, String repoName,
            long commentId, String body) {
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.editReviewComment(repoOwner, repoName, commentId, request)
                .map(ApiHelpers::throwOnFailure);
    }
}