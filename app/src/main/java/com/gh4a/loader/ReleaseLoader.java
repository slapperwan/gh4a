package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ReleaseLoader extends BaseLoader<Release> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final long mReleaseId;

    public ReleaseLoader(Context context, String repoOwner, String repoName, long id) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mReleaseId = id;
    }

    @Override
    public Release doLoadInBackground() throws IOException {
        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.getRelease(new RepositoryId(mRepoOwner, mRepoName), mReleaseId);
    }
}
