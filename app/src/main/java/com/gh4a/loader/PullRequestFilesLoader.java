package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.io.IOException;
import java.util.List;

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
    public List<GitHubFile> doLoadInBackground() throws IOException {
        return loadFiles(mRepoOwner, mRepoName, mPullRequestNumber);
    }

    public static List<GitHubFile> loadFiles(final String repoOwner, final String repoName,
            final int pullRequestNumber) throws IOException {
        final PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<GitHubFile>() {
            @Override
            public Page<GitHubFile> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getPullRequestFiles(repoOwner, repoName, pullRequestNumber, page).blockingGet());
            }
        });
    }
}
