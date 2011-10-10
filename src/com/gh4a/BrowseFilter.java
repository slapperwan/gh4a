package com.gh4a;

import android.content.Intent;
import android.os.Bundle;

import com.gh4a.utils.StringUtils;

public class BrowseFilter extends BaseActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url = getIntent().getDataString();
        
        String[] urlPart = url.split("/");
        
        String host = urlPart[2];
        
        if ("jobs.github.com".equals(host)) {
            Intent intent = new Intent().setClass(this, JobListActivity.class);
            startActivity(intent);
        }
        else {
            String first = urlPart[3];
            if ("languages".equals(first)
                    || "training".equals(first)
                    || "login".equals(first)
                    || "contact".equals(first)
                    || "features".equals(first)) {//skip this
                
            }
            else if ("explore".equals(first)) {//https://github.com/explore
                Intent intent = new Intent().setClass(this, ExploreActivity.class);
                startActivity(intent);
            }
            else if ("blog".equals(first)) {//https://github.com/blog
                Intent intent = new Intent().setClass(this, BlogListActivity.class);
                startActivity(intent);
            }
            else {
                Gh4Application context = getApplicationContext();
                if (urlPart.length == 4) {
                    String user = urlPart[3];
                    
                    context.openUserInfoActivity(this, user, null);
                }
                else if (urlPart.length == 5) {
                    String user = urlPart[3];
                    String repo = urlPart[4];
                    
                    context.openRepositoryInfoActivity(this, user, repo);
                }
                else if (urlPart.length == 6) {
                    String user = urlPart[3];
                    String repo = urlPart[4];
                    String action = urlPart[5];
                    
                    if ("issues".equals(action)) {
                        context.openIssueListActivity(this, user, repo, Constants.Issue.ISSUE_STATE_OPEN);
                    }
                    else if ("pulls".equals(action)) {
                        context.openPullRequestListActivity(this, user, repo, Constants.Issue.ISSUE_STATE_OPEN);
                    }
                    else if ("wiki".equals(action)) {
                        Intent intent = new Intent().setClass(this, WikiListActivity.class);
                        intent.putExtra(Constants.Repository.REPO_OWNER, user);
                        intent.putExtra(Constants.Repository.REPO_NAME, repo);
                        startActivity(intent);
                    }
                }
                else if (urlPart.length == 7) {
                    String user = urlPart[3];
                    String repo = urlPart[4];
                    String action = urlPart[5];
                    String id = urlPart[6];
                    
                    if ("issues".equals(action)) {
                        if (!StringUtils.isBlank(id)) {
                            try {
                                context.openIssueActivity(this, user, repo, Integer.parseInt(id));
                            }
                            catch (NumberFormatException e) {
                                // Ignore non-numeric ids
                            }
                        }
                    }
                    else if ("pull".equals(action)) {
                        if (!StringUtils.isBlank(id)) {
                            try {
                                context.openPullRequestActivity(this, user, repo, Integer.parseInt(id));
                            }
                            catch (NumberFormatException e) {
                                // Ignore non-numeric ids
                            }
                        }
                    }
                }
            }
        }
        finish();
    }
}
