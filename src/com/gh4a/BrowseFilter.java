package com.gh4a;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.gh4a.utils.StringUtils;

public class BrowseFilter extends BaseActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        List<String> parts = uri.getPathSegments();

        String first = parts.isEmpty() ? null : parts.get(0);
        if (first == null
                || "languages".equals(first)
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

            // strip off extra data like line numbers etc.
            String last = parts.get(parts.size() - 1);
            int pos = last.indexOf('#');
            if (pos >= 0) {
                parts.set(parts.size() - 1, last.substring(0, pos));
            }

            String user = first;
            String repo = parts.size() >= 2 ? parts.get(1) : null;
            String action = parts.size() >= 3 ? parts.get(2) : null;
            String id = parts.size() >= 4 ? parts.get(3) : null;

            if (repo == null && action == null) {
                context.openUserInfoActivity(this, user, null);
            }
            else if (action == null) {
                context.openRepositoryInfoActivity(this, user, repo, 0);
            }
            else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    try {
                        context.openIssueActivity(this, user, repo, Integer.parseInt(id));
                    }
                    catch (NumberFormatException e) {
                    }
                } else {
                    context.openIssueListActivity(this, user, repo, Constants.Issue.ISSUE_STATE_OPEN);
                }
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
            else if ("pull".equals(action) && !StringUtils.isBlank(id)) {
                try {
                    context.openPullRequestActivity(this, user, repo, Integer.parseInt(id));
                }
                catch (NumberFormatException e) {
                }
            }
            else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                context.openCommitInfoActivity(this, user, repo, id, 0);
            }
            else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 5) {
                String fullPath = TextUtils.join("/", parts.subList(4, parts.size()));
                context.openFileViewerActivity(this, user, repo, id, fullPath, uri.getLastPathSegment());
            }
        }
        finish();
    }
}
