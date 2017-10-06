package com.gh4a.resolver;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.FileViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.PositionalCommentBase;

import java.util.List;

public abstract class DiffLoadTask<C extends PositionalCommentBase> extends UrlLoadTask {
    protected final String mRepoOwner;
    protected final String mRepoName;
    protected final DiffHighlightId mDiffId;

    public DiffLoadTask(FragmentActivity activity, String repoOwner, String repoName,
            DiffHighlightId diffId) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mDiffId = diffId;
    }

    @Override
    protected Intent run() throws Exception {
        List<GitHubFile> files = getFiles();
        GitHubFile file = null;
        for (GitHubFile commitFile : files) {
            if (ApiHelpers.md5(commitFile.filename()).equals(mDiffId.fileHash)) {
                file = commitFile;
                break;
            }
        }

        if (file == null || mActivity.isFinishing()) {
            return null;
        }

        String sha = getSha();
        if (sha == null || mActivity.isFinishing()) {
            return null;
        }

        if (FileUtils.isImage(file.filename())) {
            return FileViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                    sha, file.filename());
        }

        return getLaunchIntent(sha, file, getComments(), mDiffId);
    }

    protected abstract List<GitHubFile> getFiles() throws Exception;
    protected abstract String getSha() throws Exception;
    protected abstract List<C> getComments() throws Exception;
    protected abstract Intent getLaunchIntent(String sha, GitHubFile file,
            List<C> comments, DiffHighlightId diffId);
}
