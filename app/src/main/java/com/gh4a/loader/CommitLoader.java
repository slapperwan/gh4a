package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import io.reactivex.Single;

public class CommitLoader extends BaseLoader<Commit> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mObjectSha;

    public CommitLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mObjectSha = sha;
    }

    @Override
    public Commit doLoadInBackground() throws ApiRequestException {
        return loadCommit(mRepoOwner, mRepoName, mObjectSha).blockingGet();
    }

    public static Single<Commit> loadCommit(String repoOwner, String repoName, String objectSha)
            throws ApiRequestException {
        RepositoryCommitService service =
                Gh4Application.get().getGitHubService(RepositoryCommitService.class);
        return service.getCommit(repoOwner, repoName, objectSha)
                .map(ApiHelpers::throwOnFailure);
    }
}
