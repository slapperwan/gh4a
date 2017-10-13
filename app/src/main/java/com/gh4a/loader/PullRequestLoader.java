package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import io.reactivex.Single;

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
    public PullRequest doLoadInBackground() throws ApiRequestException {
        return loadPullRequest(mRepoOwner, mRepoName, mPullRequestNumber).blockingGet();
    }

    public static Single<PullRequest> loadPullRequest(String repoOwner, String repoName,
            int pullRequestNumber) {
        PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        return service.getPullRequest(repoOwner, repoName, pullRequestNumber)
                .map(ApiHelpers::throwOnFailure);
    }
}
