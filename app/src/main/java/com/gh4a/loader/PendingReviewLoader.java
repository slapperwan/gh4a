package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewState;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class PendingReviewLoader extends BaseLoader<List<Review>> {
    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public PendingReviewLoader(Context context, String repoOwner, String repoName,
            int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    protected List<Review> doLoadInBackground() throws Exception {
        final PullRequestReviewService service =
                Gh4Application.get().getGitHubService(PullRequestReviewService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getReviews(mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(result -> ApiHelpers.PageIterator.filter(result, r -> r.state() == ReviewState.Pending))
                .blockingGet();
    }
}
