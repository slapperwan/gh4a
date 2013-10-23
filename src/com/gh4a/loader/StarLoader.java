package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.StarService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class StarLoader extends BaseLoader<Boolean> {

    private String mRepoOwner;
    private String mRepoName;
    private boolean mIsStarring;
    
    public StarLoader(Context context, String repoOwner, String repoName, boolean isStarring) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIsStarring = isStarring;
    }
    
    @Override
    public Boolean doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        StarService starringService = new StarService(client);
        if (mIsStarring) {
            starringService.unstar(new RepositoryId(mRepoOwner, mRepoName));
            return false;
        }
        else {
            starringService.star(new RepositoryId(mRepoOwner, mRepoName));
            return true;
        }
    }
}
