package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

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
        PullRequestService pullService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        List<Review> reviews = pullService.getReviews(new RepositoryId(mRepoOwner, mRepoName),
                mPullRequestNumber);
        Iterator<Review> iterator = reviews.iterator();
        while (iterator.hasNext()) {
            if (!Review.STATE_PENDING.equals(iterator.next().getState())) {
                iterator.remove();
            }
        }
        return reviews;
    }
}
