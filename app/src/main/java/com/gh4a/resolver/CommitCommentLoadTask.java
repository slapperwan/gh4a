package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import java.util.List;

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
            String commitSha, IntentUtils.InitialCommentMarker marker,
            boolean finishCurrentActivity) {
        super(activity, finishCurrentActivity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mCommitSha = commitSha;
        mMarker = marker;
    }

    @Override
    protected Intent run() throws Exception {
        List<CommitComment> comments =
                CommitCommentListLoader.loadComments(mRepoOwner, mRepoName, mCommitSha);
        RepositoryCommit commit = CommitLoader.loadCommit(mRepoOwner, mRepoName, mCommitSha);

        CommitFile resultFile = null;
        for (CommitComment comment : comments) {
            if (mMarker.matches(comment.getId(), comment.getCreatedAt())) {
                for (CommitFile commitFile : commit.getFiles()) {
                    if (commitFile.getFilename().equals(comment.getPath())) {
                        resultFile = commitFile;
                        break;
                    }
                }
                break;
            }
        }

        if (mActivity.isFinishing()) {
            return null;
        }

        Intent intent = null;
        if (resultFile != null) {
            if (!FileUtils.isImage(resultFile.getFilename())) {
                intent = CommitDiffViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                        mCommitSha, resultFile.getFilename(), resultFile.getPatch(), comments, -1,
                        -1, false, mMarker);
            }
        } else {
            intent = CommitActivity.makeIntent(mActivity, mRepoOwner, mRepoName, mCommitSha,
                    mMarker);
        }
        return intent;
    }
}
