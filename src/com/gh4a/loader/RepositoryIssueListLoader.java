package com.gh4a.loader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.RepositoryIssue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

/**
 * List your issues
 */
public class RepositoryIssueListLoader extends AsyncTaskLoader<List<RepositoryIssue>> {

    private Map<String, String> mFilterData;
    
    public RepositoryIssueListLoader(Context context, Map<String, String> filterData) {
        super(context);
        this.mFilterData = filterData;
    }

    @Override
    public List<RepositoryIssue> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        try {
            return issueService.getIssues(mFilterData);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
