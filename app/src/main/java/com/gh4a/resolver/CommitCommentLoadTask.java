package com.gh4a.resolver;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.Optional;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.util.List;

import io.reactivex.Single;

public class CommitCommentLoadTask extends UrlLoadTask {
    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final String mCommitSha;
    @VisibleForTesting
    protected final IntentUtils.InitialCommentMarker mMarker;

    public CommitCommentLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            String commitSha, IntentUtils.InitialCommentMarker marker) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mCommitSha = commitSha;
        mMarker = marker;
    }

    @Override
    protected Single<Optional<Intent>> getSingle() {
        return load(mActivity, mRepoOwner, mRepoName, mCommitSha, mMarker);
    }

    public static Single<Optional<Intent>> load(Context context,
            String repoOwner, String repoName, String commitSha,
            IntentUtils.InitialCommentMarker marker) {
        RepositoryCommitService commitService = ServiceFactory.get(RepositoryCommitService.class);
        RepositoryCommentService commentService =
                ServiceFactory.get(RepositoryCommentService.class);

        Single<Commit> commitSingle = commitService.getCommit(repoOwner, repoName, commitSha)
                .map(ApiHelpers::throwOnFailure);
        Single<List<GitComment>> commentSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getCommitComments(repoOwner, repoName, commitSha, page))
                .cache(); // single is used multiple times -> avoid refetching data

        Single<Optional<GitHubFile>> fileSingle = commentSingle
                .compose(RxUtils.filterAndMapToFirst(c -> marker.matches(c.id(), c.createdAt())))
                .zipWith(commitSingle, (comment, commit) -> {
                    if (comment.isPresent()) {
                        for (GitHubFile commitFile : commit.files()) {
                            if (commitFile.filename().equals(comment.get().path())) {
                                return Optional.of(commitFile);
                            }
                        }
                    }
                    return Optional.absent();
                });

        return Single.zip(commitSingle, commentSingle, fileSingle, (commit, comments, fileOpt) -> {
            GitHubFile file = fileOpt.orNull();
            if (file != null && !FileUtils.isImage(file.filename())) {
                return Optional.of(CommitDiffViewerActivity.makeIntent(context,
                        repoOwner, repoName, commitSha, file.filename(), file.patch(),
                        comments, -1, -1, false, marker));
            } else if (file == null) {
                return Optional.of(
                        CommitActivity.makeIntent(context, repoOwner, repoName, commitSha, marker));
            }
            return Optional.absent();
        });
    }
}
