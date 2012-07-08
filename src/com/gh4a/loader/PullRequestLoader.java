package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class PullRequestLoader extends AsyncTaskLoader<PullRequest> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    
    public PullRequestLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public PullRequest loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        PullRequestService pullRequestService = new PullRequestService(client);
        try {
            return pullRequestService.getPullRequest(new RepositoryId(mRepoOwner, mRepoName),
                    mPullRequestNumber);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
