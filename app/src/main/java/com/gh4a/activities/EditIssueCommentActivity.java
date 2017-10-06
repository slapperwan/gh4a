package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.AttrRes;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.service.issues.IssueCommentService;

import java.io.IOException;

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
    protected void createComment(String repoOwner, String repoName, String body,
            long replyToCommentId) throws IOException {
        int issueNumber = getIntent().getIntExtra("issue", 0);
        IssueCommentService service =
                Gh4Application.get().getGitHubService(IssueCommentService.class);
        ApiHelpers.throwOnFailure(service.createIssueComment(repoOwner, repoName, issueNumber,
                CommentRequest.builder().body(body).build()).blockingGet());
    }

    @Override
    protected void editComment(String repoOwner, String repoName, long commentId,
            String body) throws IOException {
        IssueCommentService service =
                Gh4Application.get().getGitHubService(IssueCommentService.class);
        ApiHelpers.throwOnFailure(service.editIssueComment(repoOwner, repoName, commentId,
                CommentRequest.builder().body(body).build()).blockingGet());
    }
}