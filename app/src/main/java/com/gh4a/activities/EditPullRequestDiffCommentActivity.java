package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.gh4a.AddPullRequestReviewCommentMutation;
import com.gh4a.AddPullRequestReviewMutation;
import com.gh4a.Gh4Application;
import com.gh4a.PullRequestPendingReviewQuery;
import com.gh4a.R;
import com.gh4a.type.DraftPullRequestReviewComment;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

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

    private void createReview(String pullRequestId, CommitComment comment) {
        ArrayList<DraftPullRequestReviewComment> comments = new ArrayList<>();
        comments.add(DraftPullRequestReviewComment.builder()
                .path(comment.getPath())
                .position(comment.getPosition())
                .body(comment.getBody())
                .build());

        AddPullRequestReviewMutation mutation = AddPullRequestReviewMutation.builder()
                .pullRequestId(pullRequestId)
                .comments(comments)
                .build();

        Gh4Application.get().getApolloClient().mutate(mutation)
                .enqueue(new ApolloCall.Callback<AddPullRequestReviewMutation.Data>() {
                    @Override
                    public void onResponse(
                            @Nonnull Response<AddPullRequestReviewMutation.Data> response) {
                        if (response.hasErrors()) {
                            Log.e("Reviews", "Error: " + response.errors());
                            return;
                        }
                        Log.d("Reviews", "Created review: " +
                                response.data().addPullRequestReview().pullRequestReview().id());
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e("Reviews", "Failed creating review", e);
                    }
                });
    }

    private void addComment(String pullRequestReviewId, CommitComment comment,
            long replyToCommentId) {
        AddPullRequestReviewCommentMutation.Builder builder =
                AddPullRequestReviewCommentMutation.builder()
                        .pullRequestReviewId(pullRequestReviewId)
                        .body(comment.getBody());

        if (replyToCommentId != 0L) {
            // FIXME: This doesn't work. These ids must be loaded using GraphQL API instead
            builder.inReplyTo(String.valueOf(replyToCommentId));
        } else {
            builder
                .commitOID(comment.getCommitId())
                .path(comment.getPath())
                .position(comment.getPosition());
        }

        Gh4Application.get().getApolloClient().mutate(builder.build())
                .enqueue(new ApolloCall.Callback<AddPullRequestReviewCommentMutation.Data>() {
                    @Override
                    public void onResponse(
                            @Nonnull Response<AddPullRequestReviewCommentMutation.Data> response) {
                        if (response.hasErrors()) {
                            Log.e("Reviews", "Error: " + response.errors());
                            return;
                        }
                        String body =
                                response.data().addPullRequestReviewComment().comment().body();
                        Log.d("Reviews", "Added comment: " + body);
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e("Reviews", "Failed adding comment", e);
                    }
                });
    }

    @Override
    protected void createComment(RepositoryId repoId, final CommitComment comment,
            final long replyToCommentId) throws IOException {
        int prNumber = getIntent().getIntExtra("pull_request_number", 0);
        Bundle extras = getIntent().getExtras();
        comment.setPosition(extras.getInt("position"));
        comment.setCommitId(extras.getString("commit_id"));
        comment.setPath(extras.getString("path"));

        PullRequestPendingReviewQuery query = PullRequestPendingReviewQuery.builder()
                .owner(repoId.getOwner())
                .repo(repoId.getName())
                .prNumber(prNumber)
                .build();

        Gh4Application.get().getApolloClient().query(query)
                .enqueue(new ApolloCall.Callback<PullRequestPendingReviewQuery.Data>() {
                    @Override
                    public void onResponse(
                            @Nonnull Response<PullRequestPendingReviewQuery.Data> response) {
                        if (response.hasErrors()) {
                            Log.e("Reviews", "Error: " + response.errors());
                            return;
                        }
                        PullRequestPendingReviewQuery.PullRequest pullRequest =
                                response.data().repository().pullRequest();

                        List<PullRequestPendingReviewQuery.Node> reviews =
                                pullRequest.reviews().nodes();

                        if (reviews.isEmpty()) {
                            createReview(pullRequest.id(), comment);
                        } else {
                            PullRequestPendingReviewQuery.Node pendingReview = reviews.get(0);
                            addComment(pendingReview.id(), comment, replyToCommentId);
                        }
                    }

                    @Override
                    public void onFailure(@Nonnull ApolloException e) {
                        Log.e("Reviews", "Failed getting review data", e);
                    }
                });
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);
        pullRequestService.editComment(repoId, comment);
    }
}