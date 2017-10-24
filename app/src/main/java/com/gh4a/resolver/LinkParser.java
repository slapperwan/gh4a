package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;

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

public class LinkParser {
    private static final List<String> RESERVED_NAMES = Arrays.asList(
            "apps", "integrations", "login", "logout", "marketplace", "sessions", "settings",
            "updates", "support", "contact", "about", "personal", "open-source",
            "business", "site", "security", "features"
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

        if (IGitHubConstants.HOST_GISTS.equals(uri.getHost())) {
            return parseGistLink(activity, parts);
        }

        if (parts.isEmpty()) {
            return null;
        }

        String first = parts.get(0);
        if (RESERVED_NAMES.contains(first)) {
            return null;
        }

        switch (first) {
            case "explore":
                return new ParseResult(new Intent(activity, TrendingActivity.class));
            case "blog":
                return parseBlogLink(activity, parts);
            case "orgs":
                return parseOrganizationLink(activity, parts);
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
            case "downloads":
                return new ParseResult(DownloadsActivity.makeIntent(activity, user, repo));
            case "releases":
                return parseReleaseLink(activity, parts, user, repo, id);
            case "tree":
            case "commits":
                return parseCommitsLink(activity, parts, user, repo, action);
            case "issues":
                return parseIssuesLink(activity, uri, user, repo, id, initialCommentFallback);
            case "pulls":
                return new ParseResult(IssueListActivity.makeIntent(activity, user, repo, true));
            case "wiki":
                return new ParseResult(WikiListActivity.makeIntent(activity, user, repo, null));
            case "pull":
                return parsePullRequestLink(activity, uri, parts, user,
                        repo, id, initialCommentFallback);
            case "commit":
                return parseCommitLink(activity, uri, user, repo, id, initialCommentFallback);
            case "blob":
                return parseBlobLink(activity, uri, parts, user, repo, id);
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
    private static ParseResult parseOrganizationLink(FragmentActivity activity,
            List<String> parts) {
        String org = parts.size() >= 2 ? parts.get(1) : null;
        String action = parts.size() >= 3 ? parts.get(2) : null;

        if (org == null) {
            return null;
        }
        if ("people".equals(action)) {
            return new ParseResult(OrganizationMemberListActivity.makeIntent(activity, org));
        }
        return new ParseResult(UserActivity.makeIntent(activity, org));
    }

    @NonNull
    private static ParseResult parseUserLink(FragmentActivity activity, @NonNull Uri uri,
            String user) {
        String tab = uri.getQueryParameter("tab");
        if (tab != null) {
            switch (tab) {
                case "repositories":
                    return new ParseResult(new UserReposLoadTask(activity, user, false));
                case "stars":
                    return new ParseResult(new UserReposLoadTask(activity, user, true));
                case "followers":
                    return new ParseResult(new UserFollowersLoadTask(activity, user, true));
                case "following":
                    return new ParseResult(new UserFollowersLoadTask(activity, user, false));
                default:
                    return new ParseResult(UserActivity.makeIntent(activity, user));
            }
        }
        return new ParseResult(UserActivity.makeIntent(activity, user));
    }

    @NonNull
    private static ParseResult parseReleaseLink(FragmentActivity activity, List<String> parts,
            String user, String repo, String id) {
        if ("tag".equals(id)) {
            final String release = parts.size() >= 5 ? parts.get(4) : null;
            if (release != null) {
                return new ParseResult(new ReleaseLoadTask(activity, user, repo, release));
            }
        }
        return new ParseResult(ReleaseListActivity.makeIntent(activity, user, repo));
    }

    @NonNull
    private static ParseResult parseCommitsLink(FragmentActivity activity, List<String> parts,
            String user, String repo, String action) {
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
        return new ParseResult(new RefPathDisambiguationTask(activity, user, repo, refAndPath,
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

        DiffHighlightId diffId =
                extractDiffId(uri.getFragment(), "diff-", true);

        if (diffId != null) {
            return new ParseResult(new PullRequestDiffLoadTask(activity, user, repo, diffId,
                    pullRequestNumber));
        }

        String target = parts.size() >= 5 ? parts.get(4) : null;
        int page = parsePullRequestPage(target);

        IntentUtils.InitialCommentMarker initialDiffComment =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(), "r");
        if (initialDiffComment != null) {
            return new ParseResult(new PullRequestDiffCommentLoadTask(activity, user, repo,
                    pullRequestNumber, initialDiffComment, page));
        }

        IntentUtils.InitialCommentMarker reviewMarker =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(),
                        "pullrequestreview-");
        if (reviewMarker != null) {
            return new ParseResult(new PullRequestReviewLoadTask(activity, user, repo,
                    pullRequestNumber, reviewMarker));
        }

        IntentUtils.InitialCommentMarker reviewCommentMarker =
                generateInitialCommentMarkerWithoutFallback(uri.getFragment(),
                        "discussion_r");
        if (reviewCommentMarker != null) {
            return new ParseResult(new PullRequestReviewCommentLoadTask(activity, user, repo,
                    pullRequestNumber, reviewCommentMarker, true));
        }

        DiffHighlightId reviewDiffHunkId =
                extractDiffId(uri.getFragment(), "discussion-diff-", false);
        if (reviewDiffHunkId != null) {
            return new ParseResult(new PullRequestReviewDiffLoadTask(activity, user, repo,
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
        DiffHighlightId diffId =
                extractDiffId(uri.getFragment(), "diff-", true);
        if (diffId != null) {
            return new ParseResult(new CommitDiffLoadTask(activity, user, repo, diffId, id));
        }

        IntentUtils.InitialCommentMarker initialComment = generateInitialCommentMarker(
                uri.getFragment(), "commitcomment-", initialCommentFallback);
        if (initialComment != null) {
            return new ParseResult(new CommitCommentLoadTask(activity, user, repo, id,
                    initialComment, true));
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
        return new ParseResult(new RefPathDisambiguationTask(activity, user, repo, refAndPath,
                uri.getFragment()));
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

    private static DiffHighlightId extractDiffId(String fragment, String prefix,
            boolean isMd5Hash) {
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
