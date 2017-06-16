package com.gh4a.loader;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullRequestCommentListLoader extends IssueCommentListLoader {

    public PullRequestCommentListLoader(Context context, String repoOwner,
            String repoName, int issueNumber) {
        super(context, repoOwner, repoName, issueNumber);
    }

    @Override
    public List<TimelineItem> doLoadInBackground() throws IOException {
        // Combine issue comments and pull request comments (to get comments on diff)
        List<TimelineItem> events = super.doLoadInBackground();

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        HashMap<String, CommitFile> filesByName = new HashMap<>();
        for (CommitFile file : pullRequestService.getFiles(repoId, mIssueNumber)) {
            filesByName.put(file.getFilename(), file);
        }

        LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
        List<TimelineItem.TimelineReview> reviews = new ArrayList<>();
        for (Review review : pullRequestService.getReviews(repoId, mIssueNumber)) {
            TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
            reviewsById.put(review.getId(), timelineReview);
            reviews.add(timelineReview);

            if (Review.STATE_PENDING.equals(review.getState())) {
                // For reviews with pending state we have to manually load the comments
                List<CommitComment> pendingComments =
                        pullRequestService.getReviewComments(repoId, mIssueNumber, review.getId());

                for (CommitComment pendingComment : pendingComments) {
                    CommitFile commitFile = filesByName.get(pendingComment.getPath());
                    timelineReview.addComment(pendingComment, commitFile, true);
                }
            }
        }

        List<CommitComment> commitComments = pullRequestService.getComments(repoId, mIssueNumber);

        if (!commitComments.isEmpty()) {
            Collections.sort(commitComments);

            Map<String, TimelineItem.TimelineReview> reviewsBySpecialId = new HashMap<>();

            for (CommitComment commitComment : commitComments) {
                String id = TimelineItem.Diff.getDiffHunkId(commitComment);

                TimelineItem.TimelineReview review = reviewsBySpecialId.get(id);
                if (review == null) {
                    review = reviewsById.get(commitComment.getPullRequestReviewId());
                    reviewsBySpecialId.put(id, review);
                }

                CommitFile file = filesByName.get(commitComment.getPath());
                review.addComment(commitComment, file, true);
            }
        }

        for (TimelineItem.TimelineReview review : reviews) {
            if (!review.review.getState().equals(Review.STATE_COMMENTED) ||
                    !TextUtils.isEmpty(review.review.getBody()) ||
                    !review.getDiffHunks().isEmpty()) {
                events.add(review);
            }
        }

        Collections.sort(events, TIMELINE_ITEM_COMPARATOR);

        return events;
    }
}
