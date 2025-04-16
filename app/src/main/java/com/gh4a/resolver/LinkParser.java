package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import android.text.TextUtils;

import com.gh4a.R;
import com.gh4a.activities.BlogListActivity;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CompareActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.OrganizationMemberListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.TimelineActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LinkParser {
    private static final List<String> RESERVED_NAMES = Arrays.asList(
            "about", "account", "advisories", "apps", "business", "careers", "codespaces",
            "collections", "contact", "customer-stories", "discussions", "edu", "education",
            "enterprise", "events", "explore", "features", "git-guides", "guides", "home",
            "integrations", "join", "learn", "login", "logout", "maintenance", "marketplace",
            "mobile", "new", "newsroom", "nonprofit", "open-source", "organizations", "pages",
            "personal", "plans", "press", "premium-support", "pricing", "readme", "resources",
            "security", "services", "sessions", "settings", "shop", "site","site-map", "sponsors",
            "status", "support", "team", "terms", "topics", "updates", "watching"
    );

    private LinkParser() {
    }

    /**
     * Parses the specified {@code uri} and returns a result which will tell where to redirect the
     * user.
     *
     * @return {@code null} to redirect to browser, {@code ParseResult} with specified {@code
     * intent} to redirect to activity for that intent or {@code ParseResult} with specified {@code
     * loadTask} to execute that task in background.
     */
    @Nullable
    public static ParseResult parseUri(FragmentActivity activity, @NonNull Uri uri,
            IntentUtils.InitialCommentMarker initialCommentFallback) {
        List<String> parts = new ArrayList<>(uri.getPathSegments());

        if ("gist.github.com".equals(uri.getHost())) {
            return parseGistLink(activity, parts);
        }
        if ("blog.github.com".equals(uri.getHost())) {
            return parseNewBlogLink(activity, parts);
        }

        if (!"github.com".equals(uri.getHost())) {
            return null;
        }

        if (parts.isEmpty()) {
            return null;
        }

        String first = parts.get(0);
        if (RESERVED_NAMES.contains(first)) {
            return null;
        }

        switch (first) {
            case "notifications":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.notifications));
            case "stars":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.bookmarks));
            case "issues":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.my_issues));
            case "pulls":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.my_prs));
            case "gists":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.my_gists));
            case "dashboard":
                return new ParseResult(HomeActivity.makeIntent(activity, R.id.news_feed));
            case "repositories":
            case "trending":
                return new ParseResult(new Intent(activity, TrendingActivity.class));
            case "timeline":
                return new ParseResult(new Intent(activity, TimelineActivity.class));
            case "blog":
                return parseBlogLink(activity, parts);
            case "orgs":
                return parseOrganizationLink(activity, uri, parts);
            case "search":
                return parseSearchLink(activity, uri);
        }

        //noinspection UnnecessaryLocalVariable
        String user = first;
        String repo = parts.size() >= 2 ? parts.get(1) : null;
        String action = parts.size() >= 3 ? parts.get(2) : null;
        String id = parts.size() >= 4 ? parts.get(3) : null;

        if (repo == null && action == null) {
            return parseUserLink(activity, uri, user);
        }

        if (action == null) {
            return new ParseResult(RepositoryActivity.makeIntent(activity, user, repo));
        }

        switch (action) {
            case "releases":
                return parseReleaseLink(activity, parts, user, repo, id);
            case "tree":
            case "commits":
                return parseCommitsLink(activity, uri, parts, user, repo, action);
            case "issues":
                return parseIssuesLink(activity, uri, user, repo, id, initialCommentFallback);
            case "pulls":
                return new ParseResult(IssueListActivity.makeIntent(activity, user, repo, true));
            case "wiki":
                return parseWikiLink(activity, parts, user, repo);
            case "pull":
                return parsePullRequestLink(activity, uri, parts, user, repo, id, initialCommentFallback);
            case "commit":
                return parseCommitLink(activity, uri, user, repo, id, initialCommentFallback);
            case "blob":
                return parseBlobLink(activity, uri, parts, user, repo, id);
            case "compare":
                return parseCompareLink(activity, user, repo, id);
        }
        return null;
    }

    @Nullable
    private static ParseResult parseGistLink(FragmentActivity activity, List<String> parts) {
        if (!parts.isEmpty()) {
            String gistId = parts.get(parts.size() - 1);
            return new ParseResult(GistActivity.makeIntent(activity, gistId));
        }
        return null;
    }

    @Nullable
    private static ParseResult parseBlogLink(FragmentActivity activity, List<String> parts) {
        if (parts.size() == 1) {
            return new ParseResult(new Intent(activity, BlogListActivity.class));
        }
        return null;
    }

    @Nullable
    private static ParseResult parseNewBlogLink(FragmentActivity activity, List<String> parts) {
        if (parts.size() == 0) {
            return new ParseResult(new Intent(activity, BlogListActivity.class));
        }
        return null;
    }

    @Nullable
    private static ParseResult parseOrganizationLink(FragmentActivity activity, Uri uri,
            List<String> parts) {
        String org = parts.size() >= 2 ? parts.get(1) : null;
        String action = parts.size() >= 3 ? parts.get(2) : null;

        if (org == null) {
            return null;
        }
        if (action == null) {
            return new ParseResult(UserActivity.makeIntent(activity, org));
        }
        if ("people".equals(action)) {
            return new ParseResult(OrganizationMemberListActivity.makeIntent(activity, org));
        }
        if ("repositories".equals(action)) {
            return new ParseResult(new UserReposLoadTask(activity, uri, org, false));
        }
        return null;
    }

    private static ParseResult parseUserLink(FragmentActivity activity, @NonNull Uri uri,
            String user) {
        String tab = uri.getQueryParameter("tab");
        if (tab != null) {
            switch (tab) {
                case "repositories":
                    return new ParseResult(new UserReposLoadTask(activity, uri, user, false));
                case "stars":
                    return new ParseResult(new UserReposLoadTask(activity, uri, user, true));
                case "followers":
                    return new ParseResult(new UserFollowersLoadTask(activity, uri, user, true));
                case "following":
                    return new ParseResult(new UserFollowersLoadTask(activity, uri, user, false));
                default:
                    return new ParseResult(UserActivity.makeIntent(activity, user));
            }
        }
        return new ParseResult(UserActivity.makeIntent(activity, user));
    }

    private static ParseResult parseSearchLink(FragmentActivity activity, @NonNull Uri uri) {
        String type = uri.getQueryParameter("type");
        int typeInt = SearchActivity.SEARCH_TYPE_REPO;
        if (type != null) {
            switch (type) {
                case "Repositories": typeInt = SearchActivity.SEARCH_TYPE_REPO; break;
                case "Users": typeInt = SearchActivity.SEARCH_TYPE_USER; break;
                case "Code": typeInt = SearchActivity.SEARCH_TYPE_CODE; break;
                default: return null;
            }
        }
        String query = uri.getQueryParameter("q");
        return new ParseResult(SearchActivity.makeIntent(activity, query, typeInt, true));
    }

    private static ParseResult parseReleaseLink(FragmentActivity activity, List<String> parts,
            String user, String repo, String id) {
        if ("download".equals(id)) {
            return null;
        }
        if ("tag".equals(id)) {
            String releaseTag = parts.size() >= 5 ? parts.get(4) : null;
            if (releaseTag != null) {
                return new ParseResult(ReleaseInfoActivity.makeIntent(activity, user, repo, releaseTag));
            }
        } else if (!TextUtils.isEmpty(id)) {
            // URLs like https://github.com/{user}/{repo}/releases/{id} do not exist on GitHub website:
            // this is actually an escape hatch to gracefully handle release URLs generated by "normalizing"
            // releases API URLs returned by GH notifications API.
            try {
                long numericId = Long.parseLong(id);
                return new ParseResult(ReleaseInfoActivity.makeIntent(activity, user, repo, numericId));
            } catch (NumberFormatException e) {
                // fall through to release list
            }
        }
        return new ParseResult(ReleaseListActivity.makeIntent(activity, user, repo));
    }

    @NonNull
    private static ParseResult parseCommitsLink(FragmentActivity activity, Uri uri,
            List<String> parts, String user, String repo, String action) {
        int page = "tree".equals(action)
                ? RepositoryActivity.PAGE_FILES
                : RepositoryActivity.PAGE_COMMITS;

        int refStart = 3;
        if (parts.size() >= 6
                && TextUtils.equals(parts.get(3), "refs")
                && TextUtils.equals(parts.get(4), "heads")) {
            refStart = 5;
        }
        String refAndPath = TextUtils.join("/", parts.subList(refStart, parts.size()));
        return new ParseResult(new RefPathDisambiguationTask(activity, uri, user, repo, refAndPath,
                page));
    }

    @Nullable
    private static ParseResult parseIssuesLink(FragmentActivity activity, @NonNull Uri uri,
            String user, String repo, String id,
            IntentUtils.InitialCommentMarker initialCommentFallback) {
        if (StringUtils.isBlank(id)) {
            return new ParseResult(IssueListActivity.makeIntent(activity, user, repo));
        }
        if ("new".equals(id)) {
            return new ParseResult(IssueEditActivity.makeCreateIntent(activity, user, repo));
        }
        try {
            int issueNumber = Integer.parseInt(id);
            IntentUtils.InitialCommentMarker initialComment = generateInitialCommentMarker(
                    uri.getFragment(), "issuecomment-", initialCommentFallback);
            return new ParseResult(IssueActivity.makeIntent(activity, user, repo, issueNumber,
                    initialComment));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Nullable
    private static ParseResult parseWikiLink(FragmentActivity activity, List<String> parts, String user, String repo) {
        // In this case we can't open links to specific wiki pages in a WikiActivity,
        // since there are no APIs to fetch the content of a single wiki page
        if (parts.size() > 3) {
            return null;
        }
        return new ParseResult(WikiListActivity.makeIntent(activity, user, repo, null));
    }

    @Nullable
    private static ParseResult parsePullRequestLink(FragmentActivity activity, @NonNull Uri uri,
            List<String> parts, String user, String repo, String id,
            IntentUtils.InitialCommentMarker initialCommentFallback) {
        if (StringUtils.isBlank(id)) {
            return null;
        }

        int pullRequestNumber;
        try {
            pullRequestNumber = Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return null;
        }
        if (pullRequestNumber <= 0) {
            return null;
        }

        String target = parts.size() >= 5 ? parts.get(4) : null;
        int page = parsePullRequestPage(target);

        DiffHighlightId diffId = extractDiffId(uri.getFragment(), "diff-");
        if (diffId != null) {
            return new ParseResult(new PullRequestDiffLoadTask(activity, uri, user, repo, diffId,
                    pullRequestNumber, page));
        }

        IntentUtils.InitialCommentMarker initialDiffComment =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(), "r");
        if (initialDiffComment != null) {
            return new ParseResult(new PullRequestDiffCommentLoadTask(activity, uri, user, repo,
                    pullRequestNumber, initialDiffComment, page));
        }

        IntentUtils.InitialCommentMarker reviewMarker =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(), "pullrequestreview-");
        if (reviewMarker != null) {
            return new ParseResult(new PullRequestReviewLoadTask(activity, uri, user, repo,
                    pullRequestNumber, reviewMarker));
        }

        IntentUtils.InitialCommentMarker reviewCommentMarker =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(), "discussion_r");
        if (reviewCommentMarker != null) {
            return new ParseResult(new PullRequestReviewCommentLoadTask(activity, uri, user, repo,
                    pullRequestNumber, reviewCommentMarker));
        }

        DiffHighlightId reviewDiffHunkId = extractDiffId(uri.getFragment(), "discussion-diff-");
        if (reviewDiffHunkId != null) {
            return new ParseResult(new PullRequestReviewDiffLoadTask(activity, uri, user, repo,
                    reviewDiffHunkId, pullRequestNumber));
        }

        IntentUtils.InitialCommentMarker initialComment = generateInitialCommentMarker(
                uri.getFragment(), "issuecomment-", initialCommentFallback);
        return new ParseResult(PullRequestActivity.makeIntent(activity, user, repo,
                pullRequestNumber, page, initialComment));
    }

    private static int parsePullRequestPage(String target) {
        if (target == null) {
            return -1;
        }
        switch (target) {
            case "commits":
                return PullRequestActivity.PAGE_COMMITS;
            case "files":
                return PullRequestActivity.PAGE_FILES;
        }
        return -1;
    }

    @Nullable
    private static ParseResult parseCommitLink(FragmentActivity activity, @NonNull Uri uri,
            String user, String repo, String id,
            IntentUtils.InitialCommentMarker initialCommentFallback) {
        if (StringUtils.isBlank(id)) {
            return null;
        }
        DiffHighlightId diffId = extractDiffId(uri.getFragment(), "diff-");
        if (diffId != null) {
            return new ParseResult(new CommitDiffLoadTask(activity, uri, user, repo, diffId, id));
        }

        IntentUtils.InitialCommentMarker initialComment = generateInitialCommentMarker(
                uri.getFragment(), "commitcomment-", initialCommentFallback);
        if (initialComment != null) {
            return new ParseResult(new CommitCommentLoadTask(activity, uri, user, repo, id, initialComment));
        }
        return new ParseResult(CommitActivity.makeIntent(activity, user, repo, id, null));
    }

    @Nullable
    private static ParseResult parseBlobLink(FragmentActivity activity, @NonNull Uri uri,
            List<String> parts, String user, String repo, String id) {
        if (StringUtils.isBlank(id) || parts.size() < 4) {
            return null;
        }
        String refAndPath = TextUtils.join("/", parts.subList(3, parts.size()));
        return new ParseResult(new RefPathDisambiguationTask(activity, uri, user, repo, refAndPath,
                uri.getFragment()));
    }

    @Nullable
    private static ParseResult parseCompareLink(FragmentActivity activity,
            String user, String repo, String id) {
        if (id == null) {
            return null;
        }
        String[] parts = id.split("\\.\\.\\.");
        if (parts.length != 2) {
            return null;
        }
        if (StringUtils.isBlank(parts[0]) || StringUtils.isBlank(parts[1])) {
            return null;
        }
        return new ParseResult(CompareActivity.makeIntent(activity, user, repo, parts[0], parts[1]));
    }

    private static IntentUtils.InitialCommentMarker generateInitialCommentMarkerWithoutFallback(
            String fragment, String prefix) {
        if (fragment == null || !fragment.startsWith(prefix)) {
            return null;
        }
        try {
            long commentId = Long.parseLong(fragment.substring(prefix.length()));
            return new IntentUtils.InitialCommentMarker(commentId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static IntentUtils.InitialCommentMarker generateInitialCommentMarker(
            String fragment, String prefix, IntentUtils.InitialCommentMarker fallback) {
        IntentUtils.InitialCommentMarker initialCommentMarker =
                generateInitialCommentMarkerWithoutFallback(fragment, prefix);
        return initialCommentMarker != null ? initialCommentMarker : fallback;
    }

    private static DiffHighlightId extractDiffId(String fragment, String prefix) {
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
        if (typePos < 0) {
            return new DiffHighlightId(fileHash, -1, -1, false);
        }

        try {
            char type = fragment.charAt(typePos);
            String linePart = fragment.substring(typePos + 1);
            int startLine, endLine, dashPos = linePart.indexOf("-" + type);
            if (dashPos > 0) {
                startLine = Integer.parseInt(linePart.substring(0, dashPos));
                endLine = Integer.parseInt(linePart.substring(dashPos + 2));
            } else {
                startLine = Integer.parseInt(linePart);
                endLine = startLine;
            }
            return new DiffHighlightId(fileHash, startLine, endLine, right);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static class ParseResult {
        @Nullable
        public final Intent intent;

        @Nullable
        public final UrlLoadTask loadTask;

        public ParseResult(@NonNull UrlLoadTask loadTask) {
            this.intent = null;
            this.loadTask = loadTask;
        }

        public ParseResult(@NonNull Intent intent) {
            this.intent = intent;
            this.loadTask = null;
        }
    }
}
