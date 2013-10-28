package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class RepositoryCommitsLoader extends BaseLoader<List<RepositoryCommit>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    
    public RepositoryCommitsLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<RepositoryCommit> doLoadInBackground() throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get(getContext()).getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getCommits(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);
    }
}
