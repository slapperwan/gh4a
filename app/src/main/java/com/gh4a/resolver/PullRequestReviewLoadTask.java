package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

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
    protected Intent run() throws Exception {
        PullRequestService pullRequestService = (PullRequestService) Gh4Application.get()
                .getService(Gh4Application.PULL_SERVICE);

        Review review = pullRequestService.getReview(new RepositoryId(mRepoOwner, mRepoName),
                mPullRequestNumber, mMarker.commentId);

        return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                mPullRequestNumber, review, mMarker);
    }
}
