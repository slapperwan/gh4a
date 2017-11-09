package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

import io.reactivex.Single;

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
    protected Single<Optional<Intent>> getSingle() {
        final PullRequestReviewCommentService service =
                ServiceFactory.get(PullRequestReviewCommentService.class);
        final PullRequestReviewService reviewService =
                ServiceFactory.get(PullRequestReviewService.class);
        long diffCommentId = Long.parseLong(mDiffId.fileHash);

        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filterAndMapToFirst(c -> c.id() == diffCommentId))
                .flatMap(commentOpt -> commentOpt.flatMap(comment -> {
                    long reviewId = comment.pullRequestReviewId();
                    return reviewService.getReview(mRepoOwner, mRepoName, mPullRequestNumber, reviewId)
                            .map(ApiHelpers::throwOnFailure);
                }))
                .map(reviewOpt -> reviewOpt.map(review -> ReviewActivity.makeIntent(
                        mActivity, mRepoOwner, mRepoName, mPullRequestNumber, review,
                            new IntentUtils.InitialCommentMarker(diffCommentId)))
                );
    }
}
