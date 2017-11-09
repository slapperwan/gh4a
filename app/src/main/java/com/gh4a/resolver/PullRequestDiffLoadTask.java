package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.List;

import io.reactivex.Single;

public class PullRequestDiffLoadTask extends DiffLoadTask<ReviewComment> {
    @VisibleForTesting
    protected final int mPullRequestNumber;

    public PullRequestDiffLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            DiffHighlightId diffId, int pullRequestNumber) {
        super(activity, repoOwner, repoName, diffId);
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    protected Intent getLaunchIntent(String sha, GitHubFile file,
            List<ReviewComment> comments, DiffHighlightId diffId) {
        return PullRequestDiffViewerActivity.makeIntent(mActivity, mRepoOwner,
                mRepoName, mPullRequestNumber, sha, file.filename(), file.patch(),
                comments, -1, diffId.startLine, diffId.endLine, diffId.right, null);
    }

    @Override
    protected Single<String> getSha() {
        PullRequestService service = ServiceFactory.get(PullRequestService.class, false);
        return service.getPullRequest(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(ApiHelpers::throwOnFailure)
                .map(pr -> pr.head().sha());
    }

    @Override
    protected Single<List<GitHubFile>> getFiles() {
        final PullRequestService service = ServiceFactory.get(PullRequestService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(mRepoOwner, mRepoName, mPullRequestNumber, page));
    }

    @Override
    protected Single<List<ReviewComment>> getComments() {
        final PullRequestReviewCommentService service =
                ServiceFactory.get(PullRequestReviewCommentService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() != null));
    }
}
