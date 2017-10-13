package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;

import io.reactivex.Single;

public class EditIssueCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, int issueNumber, long id, String body,
            @AttrRes int highlightColorAttr) {
        Intent intent = new Intent(context, EditIssueCommentActivity.class)
                .putExtra("issue", issueNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, id, 0L, body, highlightColorAttr);
    }

    @Override
    protected Single<GitHubCommentBase> createComment(String repoOwner, String repoName,
            String body, long replyToCommentId) {
        int issueNumber = getIntent().getIntExtra("issue", 0);
        IssueCommentService service =
                Gh4Application.get().getGitHubService(IssueCommentService.class);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.createIssueComment(repoOwner, repoName, issueNumber, request)
                .map(ApiHelpers::throwOnFailure)
                .map(response -> response);
    }

    @Override
    protected Single<GitHubCommentBase> editComment(String repoOwner, String repoName,
            long commentId, String body) {
        IssueCommentService service =
                Gh4Application.get().getGitHubService(IssueCommentService.class);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.editIssueComment(repoOwner, repoName, commentId, request)
                .map(ApiHelpers::throwOnFailure)
                .map(response -> response);
    }
}