package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Branch;
import com.meisolsson.githubsdk.model.Page;
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
    public List<Branch> doLoadInBackground() throws IOException {
        final RepositoryService service =
                Gh4Application.get().getGitHubService(RepositoryService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Branch>() {
            @Override
            public Page<Branch> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getTags(mRepoOwner, mRepoName, page).blockingGet());
            }
        });
    }
}
