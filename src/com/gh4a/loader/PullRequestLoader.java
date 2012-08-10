package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class PullRequestLoader extends BaseLoader {

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
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(app.getAuthToken());
        PullRequestService pullRequestService = new PullRequestService(client);
        result.put(LoaderResult.DATA, pullRequestService.getPullRequest(new RepositoryId(mRepoOwner, mRepoName),
                    mPullRequestNumber));
    }
}
