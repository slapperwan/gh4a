package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

public class ContributorListLoader extends BaseLoader<List<User>> {
    private final String mRepoOwner;
    private final String mRepoName;

    public ContributorListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<User> doLoadInBackground() throws IOException {
        final RepositoryService service =
                Gh4Application.get().getGitHubService(RepositoryService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<User>() {
            @Override
            public Page<User> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getContributors(mRepoOwner, mRepoName, page).blockingGet());
            }
        });
    }
}
