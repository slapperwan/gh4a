package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public List<ReviewComment> doLoadInBackground() throws IOException {
        return loadComments(mRepoOwner, mRepoName, mPullRequestNumber);
    }

    public static List<ReviewComment> loadComments(final String repoOwner, final String repoName,
            final int pullRequestNumber) throws IOException {
        final PullRequestReviewCommentService service =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);
        List<ReviewComment> comments = ApiHelpers.Pager.fetchAllPages(
                new ApiHelpers.Pager.PageProvider<ReviewComment>() {
            @Override
            public Page<ReviewComment> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getPullRequestComments(
                        repoOwner, repoName, pullRequestNumber, page).blockingGet());
            }
        });
        List<ReviewComment> result = new ArrayList<>();
        for (ReviewComment comment : comments) {
            if (comment.position() >= 0) {
                result.add(comment);
            }
        }
        return result;
    }
}
