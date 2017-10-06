package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.request.CommentRequest;
import com.meisolsson.githubsdk.model.request.pull_request.CreateReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.io.IOException;

public class EditPullRequestDiffCommentActivity extends EditCommentActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            String commitId, String path, String line, int leftLine, int rightLine, int position,
            long id, String body, int pullRequestNumber, long replyToCommentId) {
        Intent intent = new Intent(context, EditPullRequestDiffCommentActivity.class)
                .putExtra("commit_id", commitId)
                .putExtra("path", path)
                .putExtra("line", line)
                .putExtra("left_line", leftLine)
                .putExtra("right_line", rightLine)
                .putExtra("position", position)
                .putExtra("pull_request_number", pullRequestNumber);
        return EditCommentActivity.fillInIntent(intent, repoOwner, repoName,
                id, replyToCommentId, body, R.attr.colorIssueOpen);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View header = getLayoutInflater().inflate(R.layout.edit_commit_comment_header, null);
        mEditorSheet.addHeaderView(header);

        TextView line = header.findViewById(R.id.line);
        Bundle extras = getIntent().getExtras();
        line.setText(extras.getString("line"));

        TextView title = header.findViewById(R.id.title);
        title.setText(getString(R.string.commit_comment_dialog_title, extras.getInt("left_line"),
                extras.getInt("right_line")));
    }

    @Override
    protected void createComment(String repoOwner, String repoName, String body,
            long replyToCommentId) throws IOException {
        Bundle extras = getIntent().getExtras();
        int prNumber = extras.getInt("pull_request_number", 0);
        PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        CreateReviewComment.Builder builder = CreateReviewComment.builder()
                .body(body)
                .commitId(extras.getString("commit_id"))
                .path(extras.getString("path"))
                .position(extras.getInt("position"));

        if (replyToCommentId != 0) {
            builder.inReplyTo(replyToCommentId);
        }
        ApiHelpers.throwOnFailure(service.createReviewComment(
                repoOwner, repoName, prNumber, builder.build()).blockingGet());
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