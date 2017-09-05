package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

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
        PullRequestService pullRequestService = (PullRequestService) Gh4Application.get()
                .getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        List<CommitComment> comments = pullRequestService.getComments(repoId,
                mPullRequestNumber);

        long diffCommentId = Long.parseLong(mDiffId.fileHash);

        for (CommitComment comment : comments) {
            if (diffCommentId == comment.getId()) {
                long reviewId = comment.getPullRequestReviewId();

                Review review = pullRequestService.getReview(repoId, mPullRequestNumber,
                        reviewId);
                return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                        mPullRequestNumber, review,
                        new IntentUtils.InitialCommentMarker(diffCommentId));
            }
        }

        return null;
    }
}
