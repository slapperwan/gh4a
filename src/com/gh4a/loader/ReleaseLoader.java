package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ReleaseLoader extends BaseLoader<List<Release>> {

    private String mRepoOwner;
    private String mRepoName;

    public ReleaseLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    public List<Release> doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get(getContext()).getService(Gh4Application.REPO_SERVICE);
        return repoService.getReleases(new RepositoryId(mRepoOwner, mRepoName));
    }
}
