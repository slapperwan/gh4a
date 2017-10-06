package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.git.GitComment;

import java.util.List;

public class CommitDiffLoadTask extends DiffLoadTask<GitComment> {
    @VisibleForTesting
    protected final String mSha;

    public CommitDiffLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            DiffHighlightId diffId, String sha) {
        super(activity, repoOwner, repoName, diffId);
        mSha = sha;
    }

    @Override
    protected Intent getLaunchIntent(String sha, GitHubFile file,
            List<GitComment> comments, DiffHighlightId diffId) {
        return CommitDiffViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                sha, file.filename(), file.patch(), comments, diffId.startLine,
                diffId.endLine, diffId.right, null);
    }

    @Override
    public String getSha() throws Exception {
        return mSha;
    }

    @Override
    protected List<GitHubFile> getFiles() throws Exception {
        Commit commit = CommitLoader.loadCommit(mRepoOwner, mRepoName, mSha);
        return commit.files();
    }

    @Override
    protected List<GitComment> getComments() throws Exception {
        return CommitCommentListLoader.loadComments(mRepoOwner, mRepoName, mSha);
    }
}
