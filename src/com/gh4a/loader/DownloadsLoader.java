package com.gh4a.loader;

import java.util.HashMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DownloadService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class DownloadsLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    
    public DownloadsLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result)
            throws Exception {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        
        DownloadService downloadService = new DownloadService(client);
        result.put(LoaderResult.DATA, 
                downloadService.getDownloads(new RepositoryId(mRepoOwner, mRepoName)));
    }
}
