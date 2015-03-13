package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class PullRequestLoader extends BaseLoader<PullRequest> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public PullRequestLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public PullRequest doLoadInBackground() throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getPullRequest(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);
    }
}
