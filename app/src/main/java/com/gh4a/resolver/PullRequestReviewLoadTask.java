package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.gh4a.ServiceFactory;
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

    public PullRequestReviewLoadTask(FragmentActivity activity, Uri urlToResolve,
            String repoOwner, String repoName, int pullRequestNumber, IntentUtils.InitialCommentMarker marker) {
        super(activity, urlToResolve);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        PullRequestReviewService service = ServiceFactory.get(PullRequestReviewService.class, false);
        return service.getReview(mRepoOwner, mRepoName, mPullRequestNumber, mMarker.commentId)
                .map(ApiHelpers::throwOnFailure)
                .map(review -> Optional.of(ReviewActivity.makeIntent(mActivity,
                        mRepoOwner, mRepoName, mPullRequestNumber, review, mMarker)));
    }
}
