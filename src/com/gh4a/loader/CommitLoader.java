package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CommitLoader extends BaseLoader<RepositoryCommit> {
    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;

    public CommitLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mObjectSha = sha;
    }

    @Override
    public RepositoryCommit doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return commitService.getCommit(new RepositoryId(mRepoOwner, mRepoName), mObjectSha);
    }

}
