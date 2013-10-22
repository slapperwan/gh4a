package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.StarService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class StarLoader extends BaseLoader {

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
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        StarService starringService = new StarService(client);
        if (mIsStarring) {
            starringService.unstar(new RepositoryId(mRepoOwner, mRepoName));
            result.put(LoaderResult.DATA, false);
        }
        else {
            starringService.star(new RepositoryId(mRepoOwner, mRepoName));
            result.put(LoaderResult.DATA, true);
        }
    }
}
