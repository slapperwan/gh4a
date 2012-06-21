package com.gh4a.loader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class IssueListLoader extends AsyncTaskLoader<List<Issue>> {

    private String mLogin, mRepo;
    private Map<String, String> mFilterData;
    
    public IssueListLoader(Context context, String login, String repo,
            Map<String, String> filterData) {
        super(context);
        this.mLogin = login;
        this.mRepo = repo;
        this.mFilterData = filterData;
    }

    @Override
    public List<Issue> loadInBackground() {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        try {
            return issueService.getIssues(mLogin, mRepo, mFilterData);
        } catch (IOException e) {
            Log.e(Constants.LOG_TAG, e.getMessage(), e);
            return null;
        }
    }

}
