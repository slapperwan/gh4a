package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Branch;
import com.meisolsson.githubsdk.service.repositories.RepositoryBranchService;

import java.util.List;

public class BranchListLoader extends BaseLoader<List<Branch>> {
    private final String mRepoOwner;
    private final String mRepoName;

    public BranchListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Branch> doLoadInBackground() throws ApiRequestException {
        final RepositoryBranchService service =
                Gh4Application.get().getGitHubService(RepositoryBranchService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getBranches(mRepoOwner, mRepoName, page))
                .blockingGet();
    }
}
