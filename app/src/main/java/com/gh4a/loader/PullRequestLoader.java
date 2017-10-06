package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

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
        PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        return ApiHelpers.throwOnFailure(
                service.getPullRequest(repoOwner, repoName, pullRequestNumber).blockingGet());
    }
}
