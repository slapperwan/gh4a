package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

public class RepositoryLoader extends BaseLoader<Repository> {

    private final String mRepoOwner;
    private final String mRepoName;

    public RepositoryLoader(Context context, String repoOwner, String repoName) {
        super(context);
        this.mRepoOwner = repoOwner;
        this.mRepoName = repoName;
    }

    @Override
    public Repository doLoadInBackground() throws ApiRequestException {
        RepositoryService service = Gh4Application.get().getGitHubService(RepositoryService.class);
        return service.getRepository(mRepoOwner, mRepoName)
                .compose(response -> ApiHelpers.throwOnFailure(response))
                .blockingGet();
    }
}
