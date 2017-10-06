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
        List<Review> reviews = ApiHelpers.Pager.fetchAllPages(
                new ApiHelpers.Pager.PageProvider<Review>() {
            @Override
            public Page<Review> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getReviews(
                        mRepoOwner, mRepoName, mPullRequestNumber, page).blockingGet());
            }
        });
        Iterator<Review> iterator = reviews.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().state() != ReviewState.Pending) {
                iterator.remove();
            }
        }
        return reviews;
    }
}
