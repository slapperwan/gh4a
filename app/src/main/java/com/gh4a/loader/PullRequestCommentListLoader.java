package com.gh4a.loader;

import android.content.Context;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.model.ReviewState;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

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
    public List<TimelineItem> doLoadInBackground() throws ApiRequestException {
        // Combine issue comments and pull request comments (to get comments on diff)
        List<TimelineItem> events = super.doLoadInBackground();

        Gh4Application app = Gh4Application.get();
        final PullRequestService prService = app.getGitHubService(PullRequestService.class);
        final PullRequestReviewService reviewService =
                app.getGitHubService(PullRequestReviewService.class);
        final PullRequestReviewCommentService commentService =
                app.getGitHubService(PullRequestReviewCommentService.class);
        List<GitHubFile> files = ApiHelpers.PageIterator
                .toSingle(page -> prService.getPullRequestFiles(
                        mRepoOwner, mRepoName, mIssueNumber, page))
                .blockingGet();

        HashMap<String, GitHubFile> filesByName = new HashMap<>();
        for (GitHubFile file : files) {
            filesByName.put(file.filename(), file);
        }

        List<Review> prReviews = ApiHelpers.PageIterator
                .toSingle(page -> reviewService.getReviews(
                        mRepoOwner, mRepoName, mIssueNumber, page))
                .blockingGet();
        LongSparseArray<TimelineItem.TimelineReview> reviewsById = new LongSparseArray<>();
        List<TimelineItem.TimelineReview> reviews = new ArrayList<>();
        for (final Review review : prReviews) {
            TimelineItem.TimelineReview timelineReview = new TimelineItem.TimelineReview(review);
            reviewsById.put(review.id(), timelineReview);
            reviews.add(timelineReview);

            if (review.state() == ReviewState.Pending) {
                // For reviews with pending state we have to manually load the comments
                List<ReviewComment> pendingComments = ApiHelpers.PageIterator
                        .toSingle(page -> reviewService.getReviewComments(
                                mRepoOwner, mRepoName, mIssueNumber, review.id()))
                        .blockingGet();

                for (ReviewComment pendingComment : pendingComments) {
                    GitHubFile commitFile = filesByName.get(pendingComment.path());
                    timelineReview.addComment(pendingComment, commitFile, true);
                }
            }
        }

        List<ReviewComment> commitComments = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getPullRequestComments(
                        mRepoOwner, mRepoName, mIssueNumber, page))
                .blockingGet();

        if (!commitComments.isEmpty()) {
            Collections.sort(commitComments, ApiHelpers.COMMENT_COMPARATOR);

            Map<String, TimelineItem.TimelineReview> reviewsBySpecialId = new HashMap<>();

            for (ReviewComment commitComment : commitComments) {
                GitHubFile file = filesByName.get(commitComment.path());
                if (commitComment.pullRequestReviewId() == 0) {
                    events.add(new TimelineItem.TimelineComment(commitComment, file));
                } else {
                    String id = TimelineItem.Diff.getDiffHunkId(commitComment);

                    TimelineItem.TimelineReview review = reviewsBySpecialId.get(id);
                    if (review == null) {
                        review = reviewsById.get(commitComment.pullRequestReviewId());
                        reviewsBySpecialId.put(id, review);
                    }

                    review.addComment(commitComment, file, true);
                }
            }
        }

        for (TimelineItem.TimelineReview review : reviews) {
            if (review.review().state() != ReviewState.Commented ||
                    !TextUtils.isEmpty(review.review().body()) ||
                    !review.getDiffHunks().isEmpty()) {
                events.add(review);
            }
        }

        Collections.sort(events, TIMELINE_ITEM_COMPARATOR);

        return events;
    }
}
