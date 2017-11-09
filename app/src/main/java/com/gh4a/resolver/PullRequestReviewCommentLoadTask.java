package com.gh4a.resolver;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.ReviewActivity;
import com.gh4a.model.TimelineItem;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Review;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewService;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.Single;

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
            String repoName, int pullRequestNumber, IntentUtils.InitialCommentMarker marker) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        return load(mActivity, mRepoOwner, mRepoName, mPullRequestNumber, mMarker);
    }

    public static Single<Optional<Intent>> load(Context context, String repoOwner, String repoName,
            int pullRequestNumber, IntentUtils.InitialCommentMarker marker) {
        final PullRequestReviewService reviewService =
                ServiceFactory.get(PullRequestReviewService.class, false);
        final PullRequestReviewCommentService commentService =
                ServiceFactory.get(PullRequestReviewCommentService.class, false);

        return ApiHelpers.PageIterator
                .toSingle(page -> commentService.getPullRequestComments(
                        repoOwner, repoName, pullRequestNumber, page))
                // Required to have comments sorted so we can find correct review
                .compose(RxUtils.sortList(ApiHelpers.COMMENT_COMPARATOR))
                .flatMap(comments -> {
                    Map<String, ReviewComment> commentsByDiffHunkId = new HashMap<>();
                    for (ReviewComment comment : comments) {
                        String id = TimelineItem.Diff.getDiffHunkId(comment);

                        if (!commentsByDiffHunkId.containsKey(id)) {
                            // Because the comment we are looking for could be a reply to another
                            // review we have to keep track of initial comments for each diff hunk
                            commentsByDiffHunkId.put(id, comment);
                        }

                        if (marker.matches(comment.id(), null)) {
                            // Once found the comment we are looking for get a correct review id from
                            // the initial diff hunk comment
                            ReviewComment initialComment = commentsByDiffHunkId.get(id);
                            long reviewId = initialComment.pullRequestReviewId();

                            return reviewService
                                    .getReview(repoOwner, repoName, pullRequestNumber, reviewId)
                                    .map(ApiHelpers::throwOnFailure)
                                    .map(Optional::of);
                        }
                    }
                    return Single.just(Optional.<Review>absent());
                })
                .map(reviewOpt -> reviewOpt.map(review -> ReviewActivity.makeIntent(context,
                        repoOwner, repoName, pullRequestNumber, review, marker)));
    }
}
