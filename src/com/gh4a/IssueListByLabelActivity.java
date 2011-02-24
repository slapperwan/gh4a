package com.gh4a;

import java.util.HashMap;
import java.util.List;

import android.util.Log;

import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;

public class IssueListByLabelActivity extends IssueListActivity {

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[3];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;
        
        // Issues
        b = new BreadCrumbHolder();
        b.setLabel("Issues");
        b.setTag(Constants.Issue.ISSUES);
        b.setData(data);
        breadCrumbHolders[2] = b;

        String label = getIntent().getStringExtra(Constants.Issue.ISSUE_LABEL);
        createBreadcrumb("Filtered by " + label, breadCrumbHolders);
    }
    
    public String getSubTitleAfterLoaded(int numberOfIssues) {
        String label = getIntent().getStringExtra(Constants.Issue.ISSUE_LABEL);
        if (numberOfIssues != -1) {
            return "Filtered by " + label + " (" + numberOfIssues + ")";
        }
        else {
            return "Filtered by " + label;
        }
    }
    
    @Override
    public List<Issue> getIssues() throws GitHubException {
        String label = getIntent().getStringExtra(Constants.Issue.ISSUE_LABEL);
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        IssueService service = factory.createIssueService();
        label = label.replaceAll(" ", "%20");
        return service.getIssues(mUserLogin, mRepoName, label);
    }
}
