package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.gh4a.Gh4Application;
import com.gh4a.R;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.DraftPullRequestReviewComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    protected void createComment(RepositoryId repoId, CommitComment comment,
            long replyToCommentId) throws IOException {
        int prNumber = getIntent().getIntExtra("pull_request_number", 0);
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);

        Review draftReview = getPendingReview(pullRequestService, repoId, prNumber);
        List<DraftPullRequestReviewComment> draftComments =
                getPendingComments(pullRequestService, repoId, prNumber, draftReview);

        if (replyToCommentId != 0L) {
            pullRequestService.replyToComment(repoId, prNumber,
                    (int) replyToCommentId, comment.getBody());
        } else {
            Bundle extras = getIntent().getExtras();
            comment.setPosition(extras.getInt("position"));
            comment.setCommitId(extras.getString("commit_id"));
            comment.setPath(extras.getString("path"));

            draftComments.add(new DraftPullRequestReviewComment()
                    .setPath(extras.getString("path"))
                    .setPosition(extras.getInt("position"))
                    .setBody(comment.getBody()));

            Map<String, Object> map = new HashMap<>();
            map.put("commitId", extras.getString("commit_id"));
            map.put("comments", draftComments);

            if (draftReview != null) {
                if (draftReview.getBody() != null) {
                    map.put("body", draftReview.getBody());
                }

                try {
                    pullRequestService.deleteReview(repoId, prNumber, draftReview.getId());
                } catch (RequestException e) {
                    if (e.getStatus() != HttpURLConnection.HTTP_OK) {
                        throw e;
                    }
                }
            }

            pullRequestService.createReview(repoId, prNumber, map);
        }
    }

    @Nullable
    private Review getPendingReview(PullRequestService pullRequestService, RepositoryId repoId,
            int prNumber) throws IOException {
        Review pendingReview = null;
        List<Review> reviews = pullRequestService.getReviews(repoId, prNumber);
        for (Review review : reviews) {
            if (Review.STATE_PENDING.equals(review.getState())) {
                pendingReview = review;
                break;
            }
        }
        return pendingReview;
    }

    private List<DraftPullRequestReviewComment> getPendingComments(
            PullRequestService pullRequestService, RepositoryId repoId, int prNumber,
            Review draftReview) throws IOException {
        List<DraftPullRequestReviewComment> draftComments = new ArrayList<>();
        if (draftReview != null) {
            List<CommitComment> pendingComments =
                    pullRequestService.getReviewComments(repoId, prNumber, draftReview.getId());

            for (CommitComment pendingComment : pendingComments) {
                draftComments.add(new DraftPullRequestReviewComment()
                        .setPath(pendingComment.getPath())
                        .setPosition(pendingComment.getPosition())
                        .setBody(pendingComment.getBody()));
            }
        }
        return draftComments;
    }

    @Override
    protected void editComment(RepositoryId repoId, CommitComment comment) throws IOException {
        Gh4Application app = Gh4Application.get();
        PullRequestService pullRequestService = (PullRequestService)
                app.getService(Gh4Application.PULL_SERVICE);
        pullRequestService.editComment(repoId, comment);
    }
}