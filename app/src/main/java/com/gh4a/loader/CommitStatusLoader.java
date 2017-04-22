package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CommitStatusLoader extends BaseLoader<List<CommitStatus>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mSha;

    public CommitStatusLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
    }

    @Override
    public List<CommitStatus> doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        return commitService.getStatuses(new RepositoryId(mRepoOwner, mRepoName), mSha);
    }
}
