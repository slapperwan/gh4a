package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
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
    public List<User> doLoadInBackground() throws ApiRequestException {
        final RepositoryService service =
                Gh4Application.get().getGitHubService(RepositoryService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getContributors(mRepoOwner, mRepoName, page))
                .blockingGet();
    }
}
