package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Contributor;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ContributorListLoader extends BaseLoader<List<Contributor>> {

    private String mRepoOwner;
    private String mRepoName;

    public ContributorListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Contributor> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.getContributors(new RepositoryId(mRepoOwner, mRepoName), true);
    }
}
