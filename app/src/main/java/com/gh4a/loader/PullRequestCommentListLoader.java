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
        super(context, repoOwner, repoName, issueNumber, true);
    }

    @Override
    public List<TimelineItem> doLoadInBackground() throws IOException {
        // Combine issue comments and pull request comments (to get comments on diff)
        List<TimelineItem> events = super.doLoadInBackground();

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        List<CommitComment> commitComments = pullRequestService.getComments(repoId, mIssueNumber);

        HashMap<String, CommitFile> filesByName = new HashMap<>();
        for (CommitFile file : pullRequestService.getFiles(repoId, mIssueNumber)) {
            filesByName.put(file.getFilename(), file);
        }

        Map<String, TimelineItem.TimelineReview> reviewsBySpecialId = new HashMap<>();
        LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
        List<TimelineItem.TimelineReview> reviews = new ArrayList<>();
        for (Review review : pullRequestService.getReviews(repoId, mIssueNumber)) {
            TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
            reviewsById.put(review.getId(), timelineReview);
            reviews.add(timelineReview);
        }

        for (CommitComment commitComment : commitComments) {
            String id = commitComment.getOriginalCommitId() + commitComment.getPath() +
                    commitComment.getOriginalPosition();
            TimelineItem.TimelineReview review = reviewsBySpecialId.get(id);
            if (review == null) {
                review = reviewsById.get(commitComment.getPullRequestReviewId());
                reviewsBySpecialId.put(id, review);
            }

            TimelineItem.Diff reviewChunk = review.chunks.get(id);
            if (reviewChunk == null) {
                reviewChunk = new TimelineItem.Diff();
                review.chunks.put(id, reviewChunk);
            }

            CommitFile file = filesByName.get(commitComment.getPath());
            reviewChunk.comments.add(new TimelineItem.TimelineComment(commitComment, file));
        }

        for (TimelineItem.TimelineReview review : reviews) {
            if (!review.review.getState().equals(Review.STATE_COMMENTED) ||
                    !TextUtils.isEmpty(review.review.getBody()) ||
                    !review.chunks.isEmpty()) {
                events.add(review);
            }
        }

        Collections.sort(events, SORTER);

        return events;
    }
}
