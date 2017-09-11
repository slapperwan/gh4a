package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubCommentBase;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;

import io.reactivex.Single;

public class EditIssueCommentActivity extends EditCommentActivity {

    private static final String EXTRA_ISSUE = "issue";

    public static Intent makeIntent(Context context, String repoOwner,
            String repoName, int issueNumber, long id, String body,
            @AttrRes int highlightColorAttr) {
        Intent intent = new Intent(context, EditIssueCommentActivity.class)
                .putExtra(EXTRA_ISSUE, issueNumber);
        return EditCommentActivity.fillInIntent(intent,
                repoOwner, repoName, id, 0L, body, highlightColorAttr);
    }

    @Override
    protected Single<GitHubCommentBase> createComment(String repoOwner, String repoName,
            String body, long replyToCommentId) {
        int issueNumber = getIntent().getIntExtra(EXTRA_ISSUE, 0);
        IssueCommentService service = ServiceFactory.get(IssueCommentService.class, false);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.createIssueComment(repoOwner, repoName, issueNumber, request)
                .map(ApiHelpers::throwOnFailure);
    }

    @Override
    protected Single<GitHubCommentBase> editComment(String repoOwner, String repoName,
            long commentId, String body) {
        IssueCommentService service = ServiceFactory.get(IssueCommentService.class, false);
        CommentRequest request = CommentRequest.builder().body(body).build();
        return service.editIssueComment(repoOwner, repoName, commentId, request)
                .map(ApiHelpers::throwOnFailure);
    }
}