package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.loader.PullRequestFilesLoader;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.PullRequest;

import java.util.List;

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
    protected Intent run() throws Exception {
        PullRequest pullRequest =
                PullRequestLoader.loadPullRequest(mRepoOwner, mRepoName, mPullRequestNumber);
        if (pullRequest == null || mActivity.isFinishing()) {
            return null;
        }

        List<CommitComment> comments = PullRequestCommentsLoader.loadComments(mRepoOwner,
                mRepoName, mPullRequestNumber);
        if (comments == null || mActivity.isFinishing()) {
            return null;
        }

        List<CommitFile> files =
                PullRequestFilesLoader.loadFiles(mRepoOwner, mRepoName, mPullRequestNumber);
        if (files == null || mActivity.isFinishing()) {
            return null;
        }

        boolean foundComment = false;
        CommitFile resultFile = null;
        for (CommitComment comment : comments) {
            if (mMarker.matches(comment.getId(), comment.getCreatedAt())) {
                foundComment = true;
                for (CommitFile commitFile : files) {
                    if (commitFile.getFilename().equals(comment.getPath())) {
                        resultFile = commitFile;
                        break;
                    }
                }
                break;
            }
        }

        if (!foundComment || mActivity.isFinishing()) {
            return null;
        }

        Intent intent = null;
        if (resultFile != null) {
            if (!FileUtils.isImage(resultFile.getFilename())) {
                intent = PullRequestDiffViewerActivity.makeIntent(mActivity, mRepoOwner,
                        mRepoName, mPullRequestNumber, pullRequest.getHead().getSha(),
                        resultFile.getFilename(), resultFile.getPatch(), comments, -1, -1, -1,
                        false, mMarker);
            }
        } else {
            intent = PullRequestActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                    mPullRequestNumber, mPage, mMarker);
        }
        return intent;
    }
}
