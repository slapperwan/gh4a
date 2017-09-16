package com.gh4a.resolver;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.client.IGitHubConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrowseFilter extends AppCompatActivity {
    public static Intent makeRedirectionIntent(Context context, Uri uri,
            IntentUtils.InitialCommentMarker initialComment) {
        Intent intent = new Intent(context, BrowseFilter.class);
        intent.setData(uri);
        intent.putExtra("initial_comment", initialComment);
        return intent;
    }

    private static final List<String> RESERVED_NAMES = Arrays.asList(
        "apps", "integrations", "login", "logout",
        "marketplace", "sessions", "settings", "updates"
    );

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
                intent = GistActivity.makeIntent(this, parts.get(parts.size() - 1));
            }
        } else if (RESERVED_NAMES.contains(first)) { //noinspection StatementWithEmptyBody
            // forward to browser
        } else if ("explore".equals(first)) {
            intent = new Intent(this, TrendingActivity.class);
        } else if ("blog".equals(first)) {
            if (parts.size() == 1) {
                intent = new Intent(this, BlogListActivity.class);
            }
        } else if ("orgs".equals(first)) {
            String org = parts.size() >= 2 ? parts.get(1) : null;
            String action = parts.size() >= 3 ? parts.get(2) : null;

            if (org != null) {
                if (action == null) {
                    intent = UserActivity.makeIntent(this, org);
                } else if ("people".equals(action)) {
                    intent = OrganizationMemberListActivity.makeIntent(this, org);
                }
            }
        } else if (first != null) {
            String user = first;
            String repo = parts.size() >= 2 ? parts.get(1) : null;
            String action = parts.size() >= 3 ? parts.get(2) : null;
            String id = parts.size() >= 4 ? parts.get(3) : null;

            if (repo == null && action == null) {
                String tab = uri.getQueryParameter("tab");
                if (tab != null) {
                    switch (tab) {
                        case "repositories":
                            new UserReposLoadTask(this, user, false).execute();
                            return; // avoid finish() for now
                        case "stars":
                            new UserReposLoadTask(this, user, true).execute();
                            return; // avoid finish() for now
                        case "followers":
                            new UserFollowersLoadTask(this, user, true).execute();
                            return; // avoid finish() for now
                        case "following":
                            new UserFollowersLoadTask(this, user, false).execute();
                            return; // avoid finish() for now
                        default:
                            intent = UserActivity.makeIntent(this, user);
                            break;
                    }
                } else {
                    intent = UserActivity.makeIntent(this, user);
                }
            } else if (action == null) {
                intent = RepositoryActivity.makeIntent(this, user, repo);
            } else if ("downloads".equals(action)) {
                intent = DownloadsActivity.makeIntent(this, user, repo);
            } else if ("releases".equals(action)) {
                if ("tag".equals(id)) {
                    final String release = parts.size() >= 5 ? parts.get(4) : null;
                    new ReleaseLoadTask(this, user, repo, release).execute();
                    return; // avoid finish() for now
                } else {
                    intent = ReleaseListActivity.makeIntent(this, user, repo);
                }
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
                new RefPathDisambiguationTask(this, user, repo, refAndPath, page).execute();
                return; // avoid finish() for now
            } else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    if ("new".equals(id)) {
                        intent = IssueEditActivity.makeCreateIntent(this, user, repo);
                    } else {
                        try {
                            IssueActivity.startTask(this, user, repo, Integer.parseInt(id),
                                    generateInitialCommentMarker(uri.getFragment(), "issuecomment-"));
                        } catch (NumberFormatException e) {
                            // ignored
                        }
                    }
                } else {
                    intent = IssueListActivity.makeIntent(this, user, repo);
                }
            } else if ("pulls".equals(action)) {
                intent = IssueListActivity.makeIntent(this, user, repo, true);
            } else if ("wiki".equals(action)) {
                intent = WikiListActivity.makeIntent(this, user, repo, null);
            } else if ("pull".equals(action) && !StringUtils.isBlank(id)) {
                int pullRequestNumber = -1;
                try {
                    pullRequestNumber = Integer.parseInt(id);
                } catch (NumberFormatException e) {
                    // ignored
                }

                if (pullRequestNumber > 0) {
                    DiffHighlightId diffId = extractDiffId(uri.getFragment(), "diff-", true);

                    if (diffId != null) {
                        new PullRequestDiffLoadTask(this, user, repo, diffId, pullRequestNumber
                        )
                                .execute();
                        return; // avoid finish() for now
                    } else {
                        String target = parts.size() >= 5 ? parts.get(4) : null;
                        int page = "commits".equals(target) ? PullRequestActivity.PAGE_COMMITS
                                : "files".equals(target) ? PullRequestActivity.PAGE_FILES : -1;
                        IntentUtils.InitialCommentMarker initialDiffComment =
                                generateInitialCommentMarkerWithoutFallback(uri.getFragment(), "r");
                        if (initialDiffComment != null) {
                            new PullRequestDiffCommentLoadTask(this, user, repo, pullRequestNumber,
                                    initialDiffComment, page).execute();
                            return; // avoid finish() for now
                        }

                        IntentUtils.InitialCommentMarker reviewMarker =
                                generateInitialCommentMarkerWithoutFallback(uri.getFragment(),
                                        "pullrequestreview-");
                        if (reviewMarker != null) {
                            new PullRequestReviewLoadTask(this, user, repo, pullRequestNumber,
                                    reviewMarker).execute();
                            return; // avoid finish() for now
                        }

                        IntentUtils.InitialCommentMarker reviewCommentMarker =
                                generateInitialCommentMarkerWithoutFallback(uri.getFragment(),
                                        "discussion_r");
                        if (reviewCommentMarker != null) {
                            new PullRequestReviewCommentLoadTask(this, user, repo,
                                    pullRequestNumber, reviewCommentMarker, true).execute();
                            return; // avoid finish() for now
                        }

                        DiffHighlightId reviewDiffHunkId =
                                extractDiffId(uri.getFragment(), "discussion-diff-", false);
                        if (reviewDiffHunkId != null) {
                            new PullRequestReviewDiffLoadTask(this, user, repo, reviewDiffHunkId,
                                    pullRequestNumber).execute();
                            return; // avoid finish() for now
                        }

                        IntentUtils.InitialCommentMarker initialComment =
                                generateInitialCommentMarker(uri.getFragment(), "issuecomment-");
                        PullRequestActivity.startTask(this, user, repo, pullRequestNumber,
                                page, initialComment);
                    }
                }
            } else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                DiffHighlightId diffId = extractDiffId(uri.getFragment(), "diff-", true);
                if (diffId != null) {
                    new CommitDiffLoadTask(this, user, repo, diffId, id).execute();
                    return; // avoid finish() for now
                } else {
                    IntentUtils.InitialCommentMarker initialComment =
                            generateInitialCommentMarker(uri.getFragment(), "commitcomment-");
                    if (initialComment != null) {
                        new CommitCommentLoadTask(this, user, repo, id, initialComment, true)
                                .execute();
                        return; // avoid finish() for now
                    }
                    intent = CommitActivity.makeIntent(this, user, repo, id, null);
                }
            } else if ("blob".equals(action) && !StringUtils.isBlank(id) && parts.size() >= 4) {
                String refAndPath = TextUtils.join("/", parts.subList(3, parts.size()));
                new RefPathDisambiguationTask(this, user, repo, refAndPath, uri.getFragment()
                ).execute();
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

    private IntentUtils.InitialCommentMarker generateInitialCommentMarkerWithoutFallback(
            String fragment, String prefix) {
        if (fragment != null && fragment.startsWith(prefix)) {
            try {
                long commentId = Long.parseLong(fragment.substring(prefix.length()));
                return new IntentUtils.InitialCommentMarker(commentId);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return null;
    }

    private IntentUtils.InitialCommentMarker generateInitialCommentMarker(String fragment,
            String prefix) {
        IntentUtils.InitialCommentMarker initialCommentMarker =
                generateInitialCommentMarkerWithoutFallback(fragment, prefix);
        if (initialCommentMarker == null) {
            return getIntent().getParcelableExtra("initial_comment");
        }
        return initialCommentMarker;
    }

    private DiffHighlightId extractDiffId(String fragment, String prefix, boolean isMd5Hash) {
        if (fragment == null || !fragment.startsWith(prefix)) {
            return null;
        }

        boolean right = false;
        int typePos = fragment.indexOf('L', prefix.length());
        if (typePos < 0) {
            right = true;
            typePos = fragment.indexOf('R', prefix.length());
        }

        String fileHash = typePos > 0
                ? fragment.substring(prefix.length(), typePos)
                : fragment.substring(prefix.length());
        if (isMd5Hash && fileHash.length() != 32) { // MD5 hash length
            return null;
        }
        if (typePos < 0) {
            return new DiffHighlightId(fileHash, -1, -1, false);
        }

        try {
            char type = fragment.charAt(typePos);
            String linePart = fragment.substring(typePos + 1);
            int startLine, endLine, dashPos = linePart.indexOf("-" + type);
            if (dashPos > 0) {
                startLine = Integer.valueOf(linePart.substring(0, dashPos));
                endLine = Integer.valueOf(linePart.substring(dashPos + 2));
            } else {
                startLine = Integer.valueOf(linePart);
                endLine = startLine;
            }
            return new DiffHighlightId(fileHash, startLine, endLine, right);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class DiffHighlightId {
        public final String fileHash;
        public final int startLine;
        public final int endLine;
        public final boolean right;

        public DiffHighlightId(String fileHash, int startLine, int endLine, boolean right) {
            this.fileHash = fileHash;
            this.startLine = startLine;
            this.endLine = endLine;
            this.right = right;
        }
    }
}
