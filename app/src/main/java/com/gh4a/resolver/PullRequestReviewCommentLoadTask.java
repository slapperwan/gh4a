package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.Gh4Application;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.loader.TimelineItem;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.Review;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PullRequestReviewCommentLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final int mPullRequestNumber;
    @VisibleForTesting
    protected final IntentUtils.InitialCommentMarker mMarker;

    public PullRequestReviewCommentLoadTask(FragmentActivity activity, String repoOwner,
            String repoName, int pullRequestNumber, IntentUtils.InitialCommentMarker marker,
            boolean finishCurrentActivity) {
        super(activity, finishCurrentActivity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
    }

    @Override
    protected Intent run() throws Exception {
        PullRequestService pullRequestService = (PullRequestService) Gh4Application.get()
                .getService(Gh4Application.PULL_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);

        List<CommitComment> comments = pullRequestService.getComments(repoId,
                mPullRequestNumber);

        // Required to have comments sorted so we can find correct review
        Collections.sort(comments);

        Map<String, CommitComment> commentsByDiffHunkId = new HashMap<>();
        for (CommitComment comment : comments) {
            String id = TimelineItem.Diff.getDiffHunkId(comment);

            if (!commentsByDiffHunkId.containsKey(id)) {
                // Because the comment we are looking for could be a reply to another review
                // we have to keep track of initial comments for each diff hunk
                commentsByDiffHunkId.put(id, comment);
            }

            if (mMarker.matches(comment.getId(), null)) {
                // Once found the comment we are looking for get a correct review id from
                // the initial diff hunk comment
                CommitComment initialComment = commentsByDiffHunkId.get(id);
                long reviewId = initialComment.getPullRequestReviewId();

                Review review = pullRequestService.getReview(repoId, mPullRequestNumber,
                        reviewId);
                return ReviewActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                        mPullRequestNumber, review, mMarker);
            }
        }

        return null;
    }
}
