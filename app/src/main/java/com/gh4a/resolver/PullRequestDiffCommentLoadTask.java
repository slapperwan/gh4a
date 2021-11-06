package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;
import androidx.core.util.Pair;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
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

    public PullRequestDiffCommentLoadTask(FragmentActivity activity, Uri urlToResolve,
            String repoOwner, String repoName, int pullRequestNumber,
            IntentUtils.InitialCommentMarker marker, int page) {
        super(activity, urlToResolve);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
        mMarker = marker;
        mPage = page;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        PullRequestService service = ServiceFactory.get(PullRequestService.class, false);
        Single<PullRequest> pullRequestSingle = service.getPullRequest(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(ApiHelpers::throwOnFailure);

        final PullRequestReviewCommentService commentService =
                ServiceFactory.get(PullRequestReviewCommentService.class, false);

        Single<List<ReviewComment>> commentsSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() != null))
                .cache(); // single is used multiple times -> avoid refetching data

        Single<List<GitHubFile>> filesSingle = ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(mRepoOwner, mRepoName, mPullRequestNumber, page));

        return commentsSingle
                .compose(RxUtils.filterAndMapToFirst(c -> mMarker.matches(c.id(), c.createdAt())))
                .zipWith(filesSingle, (commentOpt, files) -> commentOpt.map(comment -> {
                    for (GitHubFile commitFile : files) {
                        if (commitFile.filename().equals(comment.path())) {
                            return Pair.create(true, commitFile);
                        }
                    }
                    return Pair.create(comment != null, (GitHubFile) null);
                }))
                .flatMap(result -> {
                    if (result.isPresent()) {
                        boolean foundComment = result.get().first;
                        GitHubFile file = result.get().second;
                        if (foundComment && file != null && !FileUtils.isImage(file.filename())) {
                            return Single.zip(pullRequestSingle, commentsSingle, (pr, comments) -> {
                                //noinspection CodeBlock2Expr
                                return Optional.of(PullRequestDiffViewerActivity.makeIntent(
                                        mActivity, mRepoOwner, mRepoName, mPullRequestNumber,
                                        pr.head().sha(), file.filename(), file.patch(), comments,
                                        -1, -1, -1, false, mMarker));
                            });
                        }
                        if (foundComment && file == null) {
                            return Single.just(Optional.of(PullRequestActivity.makeIntent(mActivity,
                                    mRepoOwner, mRepoName, mPullRequestNumber, mPage, mMarker)));
                        }
                    }
                    return Single.just(Optional.absent());
                });
    }
}
