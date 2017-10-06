package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.issues.IssueLabelService;

public class LabelListLoader extends BaseLoader<List<Label>> {

    private final String mRepoOwner;
    private final String mRepoName;

    public LabelListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Label> doLoadInBackground() throws IOException {
        final IssueLabelService service = Gh4Application.get().getGitHubService(IssueLabelService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Label>() {
            @Override
            public Page<Label> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getRepositoryLabels(mRepoOwner, mRepoName, page).blockingGet());
            }
        });
    }
}
