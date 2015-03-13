package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;

public class PullRequestFilesLoader extends BaseLoader<List<CommitFile>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public PullRequestFilesLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<CommitFile> doLoadInBackground() throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getFiles(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);
    }
}
