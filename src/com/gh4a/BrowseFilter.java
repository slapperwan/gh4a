package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.client.IGitHubConstants;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;

import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class BrowseFilter extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }

        List<String> parts = new ArrayList<>(uri.getPathSegments());
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
            intent = new Intent(this, TrendingActivity.class);
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
            } else if ("downloads".equals(action)) {
                intent = new Intent(this, DownloadsActivity.class);
                intent.putExtra(Constants.Repository.OWNER, user);
                intent.putExtra(Constants.Repository.NAME, repo);
            } else if ("releases".equals(action)) {
                intent = new Intent(this, ReleaseListActivity.class);
                intent.putExtra(Constants.Repository.OWNER, user);
                intent.putExtra(Constants.Repository.NAME, repo);
            } else if ("tree".equals(action)) {
                String ref = TextUtils.join("/", parts.subList(3, parts.size()));
                intent = IntentUtils.getRepoActivityIntent(this, user, repo,
                        ref, RepositoryActivity.PAGE_FILES);
            } else if ("commits".equals(action)) {
                String ref = TextUtils.join("/", parts.subList(3, parts.size()));
                intent = IntentUtils.getRepoActivityIntent(this, user, repo,
                        ref, RepositoryActivity.PAGE_COMMITS);
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
                addLineNumbersToIntentIfPresent(intent, uri.getFragment());
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 5) {
                String fullPath = TextUtils.join("/", parts.subList(4, parts.size()));
                intent = IntentUtils.getFileViewerActivityIntent(this, user, repo, id, fullPath);
                addLineNumbersToIntentIfPresent(intent, uri.getFragment());
            } else {
                IntentUtils.launchBrowser(this, uri);
            }
        }
        if (intent != null) {
            startActivity(intent);
        }
        finish();
    }

    private void addLineNumbersToIntentIfPresent(Intent intent, String fragment) {
        if (intent == null || TextUtils.isEmpty(fragment)) {
            return;
        }

        // Line numbers are encoded either in the form #L12 or #L12-14
        if (!fragment.startsWith("L")) {
            return;
        }
        try {
            int dashPos = fragment.indexOf("-");
            if (dashPos > 0) {
                intent.putExtra(Constants.Object.HIGHLIGHT_START,
                        Integer.valueOf(fragment.substring(1, dashPos)));
                intent.putExtra(Constants.Object.HIGHLIGHT_END,
                        Integer.valueOf(fragment.substring(dashPos + 1)));
            } else {
                intent.putExtra(Constants.Object.HIGHLIGHT_START,
                        Integer.valueOf(fragment.substring(1)));
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }
}
