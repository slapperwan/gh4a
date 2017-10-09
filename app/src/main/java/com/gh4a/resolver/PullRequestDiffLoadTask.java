package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.loader.PullRequestFilesLoader;
import com.gh4a.loader.PullRequestLoader;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.ReviewComment;

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
        return PullRequestLoader.loadPullRequest(mRepoOwner, mRepoName, mPullRequestNumber)
                .map(pr -> pr.head().sha());
    }

    @Override
    protected Single<List<GitHubFile>> getFiles() {
        return PullRequestFilesLoader.loadFiles(mRepoOwner, mRepoName, mPullRequestNumber);
    }

    @Override
    protected Single<List<ReviewComment>> getComments() {
        return PullRequestCommentsLoader.loadComments(mRepoOwner, mRepoName, mPullRequestNumber);
    }
}
