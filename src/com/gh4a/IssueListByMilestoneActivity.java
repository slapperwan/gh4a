/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import com.gh4a.holder.BreadCrumbHolder;

/**
 * The IssueListByLabel activity.
 */
public class IssueListByMilestoneActivity extends IssueListActivity {

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

        String milestone = getIntent().getStringExtra(Constants.Issue.ISSUE_MILESTONE_TITLE);
        createBreadcrumb("Filtered by Milestone " + milestone, breadCrumbHolders);
    }
    
    /* (non-Javadoc)
     * @see com.gh4a.IssueListActivity#getSubTitleAfterLoaded(int)
     */
    public String getSubTitleAfterLoaded(int numberOfIssues) {
        String milestone = getIntent().getStringExtra(Constants.Issue.ISSUE_MILESTONE_TITLE);
        return "Filtered by Milestone " + milestone;
    }
    
    /* (non-Javadoc)
     * @see com.gh4a.IssueListActivity#getIssues()
     */
    @Override
    public List<Issue> getIssues() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        IssueService issueService = new IssueService(client);

        int number = getIntent().getIntExtra(Constants.Issue.ISSUE_MILESTONE_NUMBER, 0);

        Map<String, String> filterData = new HashMap<String, String>();
        filterData.put("milestone", String.valueOf(number));
        
        return issueService.getIssues(mUserLogin, mRepoName, filterData);
    }
    
    public void setRowLayout() {
        mRowLayout = R.layout.row_issue_by_label;
    }
}
