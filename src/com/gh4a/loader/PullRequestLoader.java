package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class PullRequestLoader extends BaseLoader<PullRequest> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;

    public PullRequestLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public PullRequest doLoadInBackground() throws IOException {
        return loadPullRequest(mRepoOwner, mRepoName, mPullRequestNumber);
    }

    public static PullRequest loadPullRequest(String repoOwner, String repoName,
            int pullRequestNumber) throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getPullRequest(new RepositoryId(repoOwner, repoName),
                pullRequestNumber);
    }
}
