package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class RepositoryLoader extends BaseLoader<Repository> {

    private String mRepoOwner;
    private String mRepoName;

    public RepositoryLoader(Context context, String repoOwner, String repoName) {
        super(context);
        this.mRepoOwner = repoOwner;
        this.mRepoName = repoName;
    }

    @Override
    public Repository doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.getRepository(mRepoOwner, mRepoName);
    }
}
