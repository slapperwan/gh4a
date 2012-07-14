package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class MilestoneLoader extends AsyncTaskLoader<Milestone> {

    private String mRepoOwner;
    private String mRepoName;
    private int mNumber;
    
    public MilestoneLoader(Context context, String repoOwner, String repoName, int number) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mNumber = number;
    }
    
    @Override
    public Milestone loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        MilestoneService milestoneService = new MilestoneService(client);
        try {
            return milestoneService.getMilestone(mRepoOwner, mRepoName, mNumber);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
