package com.gh4a.resolver;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.gh4a.ApiRequestException;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.PositionalCommentBase;

import java.util.List;

import io.reactivex.Single;

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
    protected Intent run() throws ApiRequestException {
        List<GitHubFile> files = getFiles().blockingGet();
        GitHubFile file = null;
        for (GitHubFile commitFile : files) {
            if (ApiHelpers.md5(commitFile.filename()).equalsIgnoreCase(mDiffId.fileHash)) {
                file = commitFile;
                break;
            }
        }

        if (file == null || mActivity.isFinishing()) {
            return null;
        }

        String sha = getSha().blockingGet();
        if (sha == null || mActivity.isFinishing()) {
            return null;
        }

        if (FileUtils.isImage(file.filename())) {
            return FileViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                    sha, file.filename());
        }

        return getLaunchIntent(sha, file, getComments().blockingGet(), mDiffId);
    }

    protected abstract Single<List<GitHubFile>> getFiles();
    protected abstract Single<String> getSha();
    protected abstract Single<List<C>> getComments();
    protected abstract Intent getLaunchIntent(String sha, GitHubFile file,
            List<C> comments, DiffHighlightId diffId);
}
