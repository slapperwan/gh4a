package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.WatcherService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class WatchLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    private boolean mIsWatching;
    
    public WatchLoader(Context context, String repoOwner, String repoName, boolean isWatching) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIsWatching = isWatching;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        WatcherService watcherService = new WatcherService(client);
        if (mIsWatching) {
            watcherService.unwatch(new RepositoryId(mRepoOwner, mRepoName));
            result.put(LoaderResult.DATA, false);
        }
        else {
            watcherService.watch(new RepositoryId(mRepoOwner, mRepoName));
            result.put(LoaderResult.DATA, true);
        }
    }
}
