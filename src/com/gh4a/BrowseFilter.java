package com.gh4a;

import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.ExploreActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class BrowseFilter extends BaseSherlockFragmentActivity {

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
            
        } else if ("explore".equals(first)) {//https://github.com/explore
            Intent intent = new Intent(this, ExploreActivity.class);
            startActivity(intent);
        } else if ("blog".equals(first)) {//https://github.com/blog
            Intent intent = new Intent(this, BlogListActivity.class);
            startActivity(intent);
        } else {
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
                IntentUtils.openUserInfoActivity(this, user);
            } else if (action == null || "tree".equals(action)) {
                String ref = "tree".equals(action) ? id : null;
                IntentUtils.openRepositoryInfoActivity(this, user, repo, ref, 0);
            } else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    try {
                        IntentUtils.openIssueActivity(this, user, repo, Integer.parseInt(id));
                    } catch (NumberFormatException e) {
                    }
                } else {
                    IntentUtils.openIssueListActivity(this, user, repo, Constants.Issue.STATE_OPEN);
                }
            } else if ("pulls".equals(action)) {
                IntentUtils.openPullRequestListActivity(this, user, repo, Constants.Issue.STATE_OPEN);
            } else if ("wiki".equals(action)) {
                Intent intent = new Intent(this, WikiListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, user);
                intent.putExtra(Constants.Repository.NAME, repo);
                startActivity(intent);
            } else if ("pull".equals(action) && !StringUtils.isBlank(id)) {
                try {
                    IntentUtils.openPullRequestActivity(this, user, repo, Integer.parseInt(id));
                } catch (NumberFormatException e) {
                }
            } else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                IntentUtils.openCommitInfoActivity(this, user, repo, id, 0);
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 5) {
                String fullPath = TextUtils.join("/", parts.subList(4, parts.size()));
                IntentUtils.openFileViewerActivity(this, user, repo, id, fullPath, uri.getLastPathSegment());
            }
        }
        finish();
    }
}
