package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.client.IGitHubConstants;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.gh4a.activities.BaseSherlockFragmentActivity;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.ExploreActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class BrowseFilter extends BaseSherlockFragmentActivity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        List<String> parts = new ArrayList<String>(uri.getPathSegments());
        Intent intent = null;

        String first = parts.isEmpty() ? null : parts.get(0);
        if (IGitHubConstants.HOST_GISTS.equals(uri.getHost())) {
            if (parts.size() >= 2) {
                intent = IntentUtils.getGistActivityIntent(this, parts.get(0), parts.get(1));
            } else {
                IntentUtils.launchBrowser(this, uri);
            }
        } else if (first == null
                || "languages".equals(first)
                || "training".equals(first)
                || "login".equals(first)
                || "contact".equals(first)
                || "features".equals(first)) {
            IntentUtils.launchBrowser(this, uri);
        } else if ("explore".equals(first)) {
            intent = new Intent(this, ExploreActivity.class);
        } else if ("blog".equals(first)) {
            intent = new Intent(this, BlogListActivity.class);
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
                intent = IntentUtils.getUserActivityIntent(this, user);
            } else if (action == null) {
                intent = IntentUtils.getRepoActivityIntent(this, user, repo, null);
            } else if ("tree".equals(action)) {
                intent = IntentUtils.getRepoActivityIntent(this, user, repo,
                        id, RepositoryActivity.PAGE_FILES);
            } else if ("commits".equals(action)) {
                intent = IntentUtils.getRepoActivityIntent(this, user, repo,
                        id, RepositoryActivity.PAGE_COMMITS);
            } else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    try {
                        intent = IntentUtils.getIssueActivityIntent(this, user, repo,
                                Integer.parseInt(id));
                    } catch (NumberFormatException e) {
                        // ignored
                    }
                } else {
                    intent = IntentUtils.getIssueListActivityIntent(this, user, repo,
                            Constants.Issue.STATE_OPEN);
                }
            } else if ("pulls".equals(action)) {
                intent = IntentUtils.getPullRequestListActivityIntent(this, user, repo,
                        Constants.Issue.STATE_OPEN);
            } else if ("wiki".equals(action)) {
                intent = new Intent(this, WikiListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, user);
                intent.putExtra(Constants.Repository.NAME, repo);
            } else if ("pull".equals(action) && !StringUtils.isBlank(id)) {
                try {
                    intent = IntentUtils.getPullRequestActivityIntent(this,
                            user, repo, Integer.parseInt(id));
                } catch (NumberFormatException e) {
                    // ignored
                }
            } else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                intent = IntentUtils.getCommitInfoActivityIntent(this, user, repo, id);
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 5) {
                String fullPath = TextUtils.join("/", parts.subList(4, parts.size()));
                intent = IntentUtils.getFileViewerActivityIntent(this, user, repo, id, fullPath);
            } else {
                IntentUtils.launchBrowser(this, uri);
            }
        }
        if (intent != null) {
            startActivity(intent);
        }
        finish();
    }
}
