package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class RepositoryCommitsLoader extends BaseLoader<List<RepositoryCommit>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    
    public RepositoryCommitsLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<RepositoryCommit> doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        PullRequestService pullRequestService = new PullRequestService(client);
        return pullRequestService.getCommits(new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);
    }
}
