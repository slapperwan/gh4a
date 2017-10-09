package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.List;

import io.reactivex.Single;

public class PullRequestFilesLoader extends BaseLoader<List<GitHubFile>> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;

    public PullRequestFilesLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<GitHubFile> doLoadInBackground() throws ApiRequestException {
        return loadFiles(mRepoOwner, mRepoName, mPullRequestNumber).blockingGet();
    }

    public static Single<List<GitHubFile>> loadFiles(final String repoOwner, final String repoName,
            final int pullRequestNumber) {
        final PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(repoOwner, repoName, pullRequestNumber, page));
    }
}
