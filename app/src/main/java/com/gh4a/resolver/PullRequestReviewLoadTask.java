package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

import io.reactivex.Single;

public class PullRequestReviewLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final int mPullRequestNumber;
    @VisibleForTesting
    protected final IntentUtils.InitialCommentMarker mMarker;

    public PullRequestReviewLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            int pullRequestNumber, IntentUtils.InitialCommentMarker marker) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        PullRequestReviewService service =
                Gh4Application.get().getGitHubService(PullRequestReviewService.class);
        return service.getReview(mRepoOwner, mRepoName, mPullRequestNumber, mMarker.commentId)
                .map(ApiHelpers::throwOnFailure)
                .map(review -> Optional.of(ReviewActivity.makeIntent(mActivity,
                        mRepoOwner, mRepoName, mPullRequestNumber, review, mMarker)));
    }
}
