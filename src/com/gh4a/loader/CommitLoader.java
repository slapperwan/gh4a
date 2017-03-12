package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;

public class CommitLoader extends BaseLoader<RepositoryCommit> {
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
    public RepositoryCommit doLoadInBackground() throws IOException {
        return loadCommit(mRepoOwner, mRepoName, mObjectSha);
    }

    public static RepositoryCommit loadCommit(String repoOwner, String repoName, String objectSha)
            throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return commitService.getCommit(new RepositoryId(repoOwner, repoName), objectSha);
    }
}
