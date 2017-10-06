package com.gh4a.loader;

import java.io.IOException;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Release;
import com.meisolsson.githubsdk.service.repositories.RepositoryReleaseService;

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
        RepositoryReleaseService service =
                Gh4Application.get().getGitHubService(RepositoryReleaseService.class);
       return ApiHelpers.throwOnFailure(service.getRelease(mRepoOwner, mRepoName, mReleaseId).blockingGet());
    }
}
