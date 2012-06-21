package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class RepositoryLoader extends AsyncTaskLoader<Repository> {

    private String mRepoOwner;
    private String mRepoName;
    
    public RepositoryLoader(Context context, String repoOwner, String repoName) {
        super(context);
        this.mRepoOwner = repoOwner;
        this.mRepoName = repoName;
    }
    
    @Override
    public Repository loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        RepositoryService repoService = new RepositoryService(client);
        try {
            return repoService.getRepository(mRepoOwner, mRepoName);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
