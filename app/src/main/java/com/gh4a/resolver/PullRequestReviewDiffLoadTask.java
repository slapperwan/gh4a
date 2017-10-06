package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

import java.io.IOException;
import java.util.List;

public class PullRequestReviewDiffLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final DiffHighlightId mDiffId;
    @VisibleForTesting
    protected final int mPullRequestNumber;

    public PullRequestReviewDiffLoadTask(FragmentActivity activity, String repoOwner,
            String repoName, DiffHighlightId diffId, int pullRequestNumber) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mDiffId = diffId;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    protected Intent run() throws Exception {
        final Gh4Application app = Gh4Application.get();
        final PullRequestReviewCommentService service =
                app.getGitHubService(PullRequestReviewCommentService.class);
        final PullRequestReviewService reviewService =
                app.getGitHubService(PullRequestReviewService.class);

        List<ReviewComment> comments = ApiHelpers.Pager.fetchAllPages(
                new ApiHelpers.Pager.PageProvider<ReviewComment>() {
            @Override
            public Page<ReviewComment> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page).blockingGet());
            }
        });

        long diffCommentId = Long.parseLong(mDiffId.fileHash);

        for (ReviewComment comment : comments) {
            if (diffCommentId == comment.id()) {
                long reviewId = comment.pullRequestReviewId();

                Review review = ApiHelpers.throwOnFailure(reviewService.getReview(
                        mRepoOwner, mRepoName, mPullRequestNumber, reviewId).blockingGet());
                return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                        mPullRequestNumber, review,
                        new IntentUtils.InitialCommentMarker(diffCommentId));
            }
        }

        return null;
    }
}
