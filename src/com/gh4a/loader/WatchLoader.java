package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.WatcherService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class WatchLoader extends AsyncTaskLoader<Boolean> {

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
    public Boolean loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        WatcherService watcherService = new WatcherService(client);
        try {
            if (mIsWatching) {
                watcherService.unwatch(new RepositoryId(mRepoOwner, mRepoName));
                return false;
            }
            else {
                watcherService.watch(new RepositoryId(mRepoOwner, mRepoName));
                return true;
            }
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }
}
