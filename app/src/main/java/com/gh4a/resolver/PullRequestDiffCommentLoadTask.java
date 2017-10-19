package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.support.v4.util.Pair;

import com.gh4a.Gh4Application;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.List;

import io.reactivex.Single;

public class PullRequestDiffCommentLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final int mPullRequestNumber;
    @VisibleForTesting
    protected final IntentUtils.InitialCommentMarker mMarker;
    @VisibleForTesting
    protected final int mPage;

    public PullRequestDiffCommentLoadTask(FragmentActivity activity, String repoOwner,
            String repoName, int pullRequestNumber, IntentUtils.InitialCommentMarker marker,
            int page) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
        mPage = page;
    }

    @Override
    protected Single<Intent> getSingle() {
        PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        Single<PullRequest> pullRequestSingle = service.getPullRequest(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(ApiHelpers::throwOnFailure);

        final PullRequestReviewCommentService commentService =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);

        Single<List<ReviewComment>> commentsSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() >= 0))
                .cache(); // single is used multiple times -> avoid refetching data

        Single<List<GitHubFile>> filesSingle = ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(mRepoOwner, mRepoName, mPullRequestNumber, page));

        return commentsSingle
                .compose(RxUtils.filterAndMapToFirstOrNull(c -> mMarker.matches(c.id(), c.createdAt())))
                .zipWith(filesSingle, (comment, files) -> {
                    if (comment != null) {
                        for (GitHubFile commitFile : files) {
                            if (commitFile.filename().equals(comment.path())) {
                                return Pair.create(true, commitFile);
                            }
                        }
                    }
                    return Pair.create(comment != null, (GitHubFile) null);
                })
                .flatMap(result -> {
                    boolean foundComment = result.first;
                    GitHubFile file = result.second;
                    if (foundComment && file != null && !FileUtils.isImage(file.filename())) {
                        return Single.zip(pullRequestSingle, commentsSingle, (pr, comments) -> {
                            return PullRequestDiffViewerActivity.makeIntent(mActivity, mRepoOwner,
                                    mRepoName, mPullRequestNumber, pr.head().sha(),
                                    file.filename(), file.patch(), comments, -1, -1, -1,
                                    false, mMarker);
                        });
                    }
                    if (foundComment && file == null) {
                        return Single.just(PullRequestActivity.makeIntent(mActivity,
                                mRepoOwner, mRepoName, mPullRequestNumber, mPage, mMarker));
                    }
                    return Single.just(null);
                });
    }
}
