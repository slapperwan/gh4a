package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
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
    protected Single<Intent> getSingle() {
        final Gh4Application app = Gh4Application.get();
        final PullRequestReviewCommentService service =
                app.getGitHubService(PullRequestReviewCommentService.class);
        final PullRequestReviewService reviewService =
                app.getGitHubService(PullRequestReviewService.class);
        long diffCommentId = Long.parseLong(mDiffId.fileHash);

        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filterAndMapToFirstOrNull(c -> c.id() == diffCommentId))
                .flatMap(comment -> {
                    if (comment != null) {
                        return Single.just(null);
                    }
                    long reviewId = comment.pullRequestReviewId();
                    return reviewService.getReview(mRepoOwner, mRepoName, mPullRequestNumber, reviewId)
                            .map(ApiHelpers::throwOnFailure);
                })
                .map(review -> {
                    if (review == null) {
                        return null;
                    }
                    return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                            mPullRequestNumber, review,
                            new IntentUtils.InitialCommentMarker(diffCommentId));

                });
    }
}
