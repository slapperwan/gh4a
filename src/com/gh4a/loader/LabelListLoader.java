package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.LabelService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class LabelListLoader extends AsyncTaskLoader<List<Label>> {

    private String mRepoOwner;
    private String mRepoName;
    
    public LabelListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public List<Label> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        LabelService labelService = new LabelService(client);
        
        try {
            return labelService.getLabels(mRepoOwner, mRepoName);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
