package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryCommit;

import java.util.List;

public class CommitDiffLoadTask extends DiffLoadTask {
    @VisibleForTesting
    protected String mSha;

    public CommitDiffLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            DiffHighlightId diffId, String sha) {
        super(activity, repoOwner, repoName, diffId);
        mSha = sha;
    }

    @Override
    protected Intent getLaunchIntent(String sha, CommitFile file, List<CommitComment> comments,
            DiffHighlightId diffId) {
        return CommitDiffViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                sha, file.getFilename(), file.getPatch(), comments, diffId.startLine,
                diffId.endLine, diffId.right, null);
    }

    @Override
    public String getSha() throws Exception {
        return mSha;
    }

    @Override
    protected List<CommitFile> getFiles() throws Exception {
        RepositoryCommit commit = CommitLoader.loadCommit(mRepoOwner, mRepoName, mSha);
        return commit.getFiles();
    }

    @Override
    protected List<CommitComment> getComments() throws Exception {
        return CommitCommentListLoader.loadComments(mRepoOwner, mRepoName, mSha);
    }
}
