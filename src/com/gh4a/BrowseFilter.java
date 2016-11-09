package com.gh4a;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Pair;

import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

public class BrowseFilter extends AppCompatActivity {
    private static final Pattern SHA1_PATTERN = Pattern.compile("[a-z0-9]{40}");

    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME == R.style.DarkTheme
                ? R.style.TransparentDarkTheme : R.style.TransparentLightTheme);

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
            if (!parts.isEmpty()) {
                intent = IntentUtils.getGistActivityIntent(this, parts.get(parts.size() - 1));
            }
        } else if ("explore".equals(first)) {
            intent = new Intent(this, TrendingActivity.class);
        } else if ("blog".equals(first)) {
            intent = new Intent(this, BlogListActivity.class);
        } else if (first != null) {
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
            } else if ("tree".equals(action) || "commits".equals(action)) {
                int page = "tree".equals(action)
                        ? RepositoryActivity.PAGE_FILES : RepositoryActivity.PAGE_COMMITS;
                int refStart = 3;
                if (parts.size() >= 6
                        && TextUtils.equals(parts.get(3), "refs")
                        && TextUtils.equals(parts.get(4), "heads")) {
                    refStart = 5;
                }
                String refAndPath = TextUtils.join("/", parts.subList(refStart, parts.size()));
                new RefPathDisambiguationTask(user, repo, refAndPath, page).execute();
                return; // avoid finish() for now
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
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 4) {
                String refAndPath = TextUtils.join("/", parts.subList(3, parts.size()));
                new RefPathDisambiguationTask(user, repo, refAndPath, uri.getFragment()).execute();
                return; // avoid finish() for now
            }
        }
        if (intent != null) {
            startActivity(intent);
        } else {
            IntentUtils.launchBrowser(this, uri);
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
            int dashPos = fragment.indexOf("-L");
            if (dashPos > 0) {
                intent.putExtra(Constants.Object.HIGHLIGHT_START,
                        Integer.valueOf(fragment.substring(1, dashPos)));
                intent.putExtra(Constants.Object.HIGHLIGHT_END,
                        Integer.valueOf(fragment.substring(dashPos + 2)));
            } else {
                intent.putExtra(Constants.Object.HIGHLIGHT_START,
                        Integer.valueOf(fragment.substring(1)));
            }
        } catch (NumberFormatException e) {
            // ignore
        }
    }

    public static class ProgressDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog pd = new ProgressDialog(getActivity());
            pd.setMessage(getString(R.string.loading_msg));
            return pd;
        }
        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            Activity activity = getActivity();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    private class RefPathDisambiguationTask extends BackgroundTask<Pair<String, String>> {
        private String mRepoOwner;
        private String mRepoName;
        private String mRefAndPath;
        private int mInitialPage;
        private String mFragment;
        private boolean mGoToFileViewer;

        public RefPathDisambiguationTask(String repoOwner, String repoName,
                String refAndPath, int initialPage) {
            super(BrowseFilter.this);
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mRefAndPath = refAndPath;
            mInitialPage = initialPage;
            mGoToFileViewer = false;
        }

        public RefPathDisambiguationTask(String repoOwner, String repoName,
                String refAndPath, String fragment) {
            super(BrowseFilter.this);
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mRefAndPath = refAndPath;
            mFragment = fragment;
            mGoToFileViewer = true;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new ProgressDialogFragment().show(getSupportFragmentManager(), "progress");
        }

        @Override
        // returns ref, path
        protected Pair<String, String> run() throws Exception {
            RepositoryService repoService = (RepositoryService)
                    Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
            RepositoryId repo = new RepositoryId(mRepoOwner, mRepoName);

            // try branches first
            List<RepositoryBranch> branches = repoService.getBranches(repo);
            if (branches != null) {
                for (RepositoryBranch branch : branches) {
                    if (TextUtils.equals(mRefAndPath, branch.getName())) {
                        return Pair.create(branch.getName(), null);
                    } else {
                        String nameWithSlash = branch.getName() + "/";
                        if (mRefAndPath.startsWith(nameWithSlash)) {
                            return Pair.create(branch.getName(),
                                    mRefAndPath.substring(nameWithSlash.length()));
                        }
                    }
                }
            }

            if (isFinishing()) {
                return null;
            }

            // and tags second
            List<RepositoryTag> tags = repoService.getTags(repo);
            if (tags != null) {
                for (RepositoryTag tag : tags) {
                    if (TextUtils.equals(mRefAndPath, tag.getName())) {
                        return Pair.create(tag.getName(), null);
                    } else {
                        String nameWithSlash = tag.getName() + "/";
                        if (mRefAndPath.startsWith(nameWithSlash)) {
                            return Pair.create(tag.getName(),
                                    mRefAndPath.substring(nameWithSlash.length()));
                        }
                    }
                }
            }

            // at this point, the first item may still be a SHA1 - check with a simple regex
            int slashPos = mRefAndPath.indexOf('/');
            if (slashPos > 0) {
                String potentialSha = mRefAndPath.substring(0, slashPos);
                if (SHA1_PATTERN.matcher(potentialSha).matches()) {
                    return Pair.create(potentialSha, mRefAndPath.substring(slashPos + 1));
                }
            }

            return null;
        }

        @Override
        protected void onSuccess(Pair<String, String> result) {
            if (isFinishing()) {
                // dialog was dismissed
                return;
            }
            Intent intent = null;
            if (result != null) {
                if (mGoToFileViewer && result.second != null) {
                    intent = IntentUtils.getFileViewerActivityIntent(BrowseFilter.this,
                            mRepoOwner, mRepoName, result.first, result.second);
                    addLineNumbersToIntentIfPresent(intent, mFragment);
                } else if (!mGoToFileViewer) {
                    intent = IntentUtils.getRepoActivityIntent(BrowseFilter.this,
                            mRepoOwner, mRepoName, result.first, result.second, mInitialPage);
                }
            }
            if (intent != null) {
                startActivity(intent);
            } else {
                IntentUtils.launchBrowser(BrowseFilter.this, getIntent().getData());
            }
            finish();
        }

        @Override
        protected void onError(Exception e) {
            IntentUtils.launchBrowser(BrowseFilter.this, getIntent().getData());
            finish();
        }
    }
}
