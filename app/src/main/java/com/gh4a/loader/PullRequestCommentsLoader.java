package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.util.List;

import io.reactivex.Single;

public class PullRequestCommentsLoader extends BaseLoader<List<ReviewComment>> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;

    public PullRequestCommentsLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<ReviewComment> doLoadInBackground() throws ApiRequestException {
        return loadComments(mRepoOwner, mRepoName, mPullRequestNumber).blockingGet();
    }

    public static Single<List<ReviewComment>> loadComments(final String repoOwner,
            final String repoName, final int pullRequestNumber) throws ApiRequestException {
        final PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestComments(
                        repoOwner, repoName, pullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() >= 0));
    }
}
