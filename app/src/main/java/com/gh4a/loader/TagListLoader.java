package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Branch;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

public class TagListLoader extends BaseLoader<List<Branch>> {
    private final String mRepoOwner;
    private final String mRepoName;

    public TagListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Branch> doLoadInBackground() throws ApiRequestException {
        final RepositoryService service =
                Gh4Application.get().getGitHubService(RepositoryService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getTags(mRepoOwner, mRepoName, page))
                .blockingGet();
    }
}
