package com.gh4a.resolver;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.gh4a.activities.FileViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;

import java.util.List;

public abstract class DiffLoadTask extends UrlLoadTask {
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
        List<CommitFile> files = getFiles();
        CommitFile file = null;
        for (CommitFile commitFile : files) {
            if (ApiHelpers.md5(commitFile.getFilename()).equals(mDiffId.fileHash)) {
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

        if (FileUtils.isImage(file.getFilename())) {
            return FileViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                    sha, file.getFilename());
        }

        return getLaunchIntent(sha, file, getComments(), mDiffId);
    }

    protected abstract List<CommitFile> getFiles() throws Exception;
    protected abstract String getSha() throws Exception;
    protected abstract List<CommitComment> getComments() throws Exception;
    protected abstract Intent getLaunchIntent(String sha, CommitFile file,
            List<CommitComment> comments, DiffHighlightId diffId);
}
