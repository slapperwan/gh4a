package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.Status;
import com.meisolsson.githubsdk.service.repositories.RepositoryStatusService;

public class CommitStatusLoader extends BaseLoader<List<Status>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mSha;

    public CommitStatusLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
    }

    @Override
    public List<Status> doLoadInBackground() throws IOException {
        final RepositoryStatusService service =
                Gh4Application.get().getGitHubService(RepositoryStatusService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Status>() {
            @Override
            public Page<Status> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getStatuses(mRepoOwner, mRepoName, mSha, page).blockingGet());
            }
        });
    }
}
