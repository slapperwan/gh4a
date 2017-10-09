package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

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
    protected Intent run() throws ApiRequestException {
        PullRequestReviewService service =
                Gh4Application.get().getGitHubService(PullRequestReviewService.class);

        Review review = ApiHelpers.throwOnFailure(service.getReview(mRepoOwner, mRepoName,
                mPullRequestNumber, mMarker.commentId).blockingGet());

        return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                mPullRequestNumber, review, mMarker);
    }
}
