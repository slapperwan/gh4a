package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class ContentLoader extends AsyncTaskLoader<Content> {

    private String mRepoOwner;
    private String mRepoName;
    private String mPath;
    private String mRef;
    
    public ContentLoader(Context context, String repoOwner, String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }
    
    @Override
    public Content loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        ContentService contentService = new ContentService(client);
        try {
            return contentService.getContent(new RepositoryId(mRepoOwner, mRepoName), mPath, mRef);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
