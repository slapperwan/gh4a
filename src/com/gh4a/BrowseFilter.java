package com.gh4a;

import android.app.Activity;
import android.app.Dialog;
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
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.activities.DownloadsActivity;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.GistActivity;
import com.gh4a.activities.IssueActivity;
import com.gh4a.activities.IssueEditActivity;
import com.gh4a.activities.IssueListActivity;
import com.gh4a.activities.PullRequestActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.activities.ReleaseInfoActivity;
import com.gh4a.activities.ReleaseListActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.TrendingActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.activities.WikiListActivity;
import com.gh4a.loader.CommitCommentListLoader;
import com.gh4a.loader.CommitLoader;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.loader.PullRequestFilesLoader;
import com.gh4a.loader.PullRequestLoader;
import com.gh4a.loader.ReleaseListLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Release;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.IGitHubConstants;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

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
                intent = GistActivity.makeIntent(this, parts.get(parts.size() - 1));
            }
        } else if ("login".equals(first) || "sessions".equals(first)) {
            // may happen during login -> forward to browser
        } else if ("explore".equals(first)) {
            intent = new Intent(this, TrendingActivity.class);
        } else if ("blog".equals(first)) {
            if (parts.size() == 1) {
                intent = new Intent(this, BlogListActivity.class);
            }
        } else if (first != null) {
            String user = first;
            String repo = parts.size() >= 2 ? parts.get(1) : null;
            String action = parts.size() >= 3 ? parts.get(2) : null;
            String id = parts.size() >= 4 ? parts.get(3) : null;

            if (repo == null && action == null) {
                intent = UserActivity.makeIntent(this, user);
            } else if (action == null) {
                intent = RepositoryActivity.makeIntent(this, user, repo);
            } else if ("downloads".equals(action)) {
                intent = DownloadsActivity.makeIntent(this, user, repo);
            } else if ("releases".equals(action)) {
                if ("tag".equals(id)) {
                    final String release = parts.size() >= 5 ? parts.get(4) : null;
                    new ReleaseLoadTask(user, repo, release).execute();
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
                new RefPathDisambiguationTask(user, repo, refAndPath, page).execute();
                return; // avoid finish() for now
            } else if ("issues".equals(action)) {
                if (!StringUtils.isBlank(id)) {
                    if ("new".equals(id)) {
                        intent = IssueEditActivity.makeCreateIntent(this, user, repo);
                    } else {
                        try {
                            Date lastReadAt =
                                    (Date) getIntent().getSerializableExtra("last_read_at");
                            intent = IssueActivity.makeIntent(this, user, repo,
                                    Integer.parseInt(id),
                                    extractCommentId(uri.getFragment(), "issue"), lastReadAt);
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
                    DiffHighlightId diffId = extractCommitDiffId(uri.getFragment());

                    if (diffId != null) {
                        new PullRequestDiffLoadTask(user, repo, diffId, pullRequestNumber)
                                .execute();
                        return; // avoid finish() for now
                    } else {
                        String target = parts.size() >= 5 ? parts.get(4) : null;
                        int page = "commits".equals(action) ? PullRequestActivity.PAGE_COMMITS
                                : "files".equals(target) ? PullRequestActivity.PAGE_FILES
                                : -1;
                        Date lastReadAt = (Date) getIntent().getSerializableExtra("last_read_at");
                        intent = PullRequestActivity.makeIntent(this, user, repo, pullRequestNumber,
                                page, extractCommentId(uri.getFragment(), "issue"), lastReadAt);
                    }
                }
            } else if ("commit".equals(action) && !StringUtils.isBlank(id)) {
                DiffHighlightId diffId = extractCommitDiffId(uri.getFragment());
                if (diffId != null) {
                    new CommitDiffLoadTask(user, repo, diffId, id).execute();
                    return; // avoid finish() for now
                } else {
                    long commentId = extractCommentId(uri.getFragment(), "commit");
                    if (commentId != -1) {
                        new CommitCommentLoadTask(user, repo, id, commentId).execute();
                        return; // avoid finish() for now
                    }
                    intent = CommitActivity.makeIntent(this, user, repo, id,
                            extractCommentId(uri.getFragment(), "commit"));
                }
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

    private long extractCommentId(String fragment, String type) {
        String prefix = type + "comment-";
        if (fragment != null && fragment.startsWith(prefix)) {
            try {
                return Long.parseLong(fragment.substring(prefix.length()));
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return -1;
    }

    private DiffHighlightId extractCommitDiffId(String fragment) {
        String prefix = "diff-";
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
        if (fileHash.length() != 32) { // MD5 hash length
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

    public static class ProgressDialogFragment extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return UiUtils.createProgressDialog(getActivity(), R.string.loading_msg);
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

    private abstract class UrlLoadTask extends BackgroundTask<Intent> {
        public UrlLoadTask() {
            super(BrowseFilter.this);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            new ProgressDialogFragment().show(getSupportFragmentManager(), "progress");
        }

        @Override
        protected void onSuccess(Intent result) {
            if (isFinishing()) {
                return;
            }

            if (result != null) {
                startActivity(result);
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

    private class ReleaseLoadTask extends UrlLoadTask {
        private final String mRepoOwner;
        private final String mRepoName;
        private final String mTagName;

        public ReleaseLoadTask(String repoOwner, String repoName, String tagName) {
            super();
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mTagName = tagName;
        }

        @Override
        protected Intent run() throws Exception {
            List<Release> releases = ReleaseListLoader.loadReleases(mRepoOwner, mRepoName);

            if (releases != null) {
                for (Release release : releases) {
                    if (TextUtils.equals(release.getTagName(), mTagName)) {
                        return ReleaseInfoActivity.makeIntent(BrowseFilter.this,
                                mRepoOwner, mRepoName, release);
                    }
                }
            }

            return null;
        }
    }

    private class CommitCommentLoadTask extends UrlLoadTask {
        private final String mRepoOwner;
        private final String mRepoName;
        private final String mCommitSha;
        private final long mCommentId;

        public CommitCommentLoadTask(String repoOwner, String repoName, String commitSha,
                long commentId) {
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mCommitSha = commitSha;
            mCommentId = commentId;
        }

        @Override
        protected Intent run() throws Exception {
            List<CommitComment> comments =
                    CommitCommentListLoader.loadComments(mRepoOwner, mRepoName, mCommitSha);
            RepositoryCommit commit = CommitLoader.loadCommit(mRepoOwner, mRepoName, mCommitSha);

            boolean foundComment = false;
            CommitFile resultFile = null;
            for (CommitComment comment : comments) {
                if (comment.getId() == mCommentId) {
                    foundComment = true;
                    for (CommitFile commitFile : commit.getFiles()) {
                        if (commitFile.getFilename().equals(comment.getPath())) {
                            resultFile = commitFile;
                            break;
                        }
                    }
                    break;
                }
            }

            if (!foundComment || isFinishing()) {
                return null;
            }

            Intent intent = null;
            if (resultFile != null) {
                if (!FileUtils.isImage(resultFile.getFilename())) {
                    intent = CommitDiffViewerActivity.makeIntent(BrowseFilter.this, mRepoOwner,
                            mRepoName, mCommitSha, resultFile.getFilename(), resultFile.getPatch(),
                            comments, -1, -1, false, mCommentId);
                }
            } else {
                intent = CommitActivity.makeIntent(BrowseFilter.this, mRepoOwner, mRepoName,
                        mCommitSha, mCommentId);
            }
            return intent;
        }
    }

    private abstract class DiffLoadTask extends UrlLoadTask {
        protected final String mRepoOwner;
        protected final String mRepoName;
        protected final DiffHighlightId mDiffId;

        public DiffLoadTask(String repoOwner, String repoName, DiffHighlightId diffId) {
            super();
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mDiffId = diffId;
        }

        @Override
        protected Intent run() throws Exception {
            List<CommitFile> files = getFiles();
            CommitFile file = null;
            for (CommitFile commitFile : files) {
                if (ApiHelpers.md5(commitFile.getFilename()).equals(mDiffId.fileHash)) {
                    file = commitFile;
                    break;
                }
            }

            if (file == null || isFinishing()) {
                return null;
            }

            String sha = getSha();
            if (sha == null || isFinishing()) {
                return null;
            }

            if (FileUtils.isImage(file.getFilename())) {
                return FileViewerActivity.makeIntent(BrowseFilter.this, mRepoOwner, mRepoName,
                        sha, file.getFilename());
            }

            return getLaunchIntent(sha, file, getComments(), mDiffId);
        }

        protected abstract List<CommitFile> getFiles() throws Exception;
        protected abstract String getSha() throws Exception;
        protected abstract List<CommitComment> getComments() throws Exception;
        protected abstract Intent getLaunchIntent(String sha, CommitFile file,
                List<CommitComment> comments, DiffHighlightId diffId);
    }

    private class PullRequestDiffLoadTask extends DiffLoadTask {
        private final int mPullRequestNumber;

        public PullRequestDiffLoadTask(String repoOwner, String repoName,
                DiffHighlightId diffId, int pullRequestNumber) {
            super(repoOwner, repoName, diffId);
            mPullRequestNumber = pullRequestNumber;
        }

        @Override
        protected Intent getLaunchIntent(String sha, CommitFile file,
                List<CommitComment> comments, DiffHighlightId diffId) {
            return PullRequestDiffViewerActivity.makeIntent(BrowseFilter.this, mRepoOwner,
                    mRepoName, mPullRequestNumber, sha, file.getFilename(), file.getPatch(),
                    comments, -1, diffId.startLine, diffId.endLine, diffId.right);
        }

        @Override
        protected String getSha() throws Exception {
            PullRequest pullRequest = PullRequestLoader.loadPullRequest(mRepoOwner, mRepoName,
                    mPullRequestNumber);
            return pullRequest.getHead().getSha();
        }

        @Override
        protected List<CommitFile> getFiles() throws Exception {
            return PullRequestFilesLoader.loadFiles(mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected List<CommitComment> getComments() throws Exception {
            return PullRequestCommentsLoader.loadComments(mRepoOwner, mRepoName,
                    mPullRequestNumber);
        }
    }

    private class CommitDiffLoadTask extends DiffLoadTask {
        private String mSha;

        public CommitDiffLoadTask(String repoOwner, String repoName,
                DiffHighlightId diffId, String sha) {
            super(repoOwner, repoName, diffId);
            mSha = sha;
        }

        @Override
        protected Intent getLaunchIntent(String sha, CommitFile file,
                List<CommitComment> comments, DiffHighlightId diffId) {
            return CommitDiffViewerActivity.makeIntent(BrowseFilter.this, mRepoOwner, mRepoName,
                    sha, file.getFilename(), file.getPatch(), comments, diffId.startLine,
                    diffId.endLine, diffId.right, -1);
        }

        @Override
        public String getSha() throws Exception {
            return mSha;
        }

        @Override
        protected List<CommitFile> getFiles() throws Exception {
            RepositoryCommit commit = CommitLoader.loadCommit(mRepoOwner, mRepoName, mSha);
            return commit.getFiles();
        }

        @Override
        protected List<CommitComment> getComments() throws Exception {
            return CommitCommentListLoader.loadComments(mRepoOwner, mRepoName, mSha);
        }
    }

    private class RefPathDisambiguationTask extends UrlLoadTask {
        private final String mRepoOwner;
        private final String mRepoName;
        private final String mRefAndPath;
        private final int mInitialPage;
        private final String mFragment;
        private final boolean mGoToFileViewer;

        public RefPathDisambiguationTask(String repoOwner, String repoName,
                String refAndPath, int initialPage) {
            super();
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mRefAndPath = refAndPath;
            mInitialPage = initialPage;
            mFragment = null;
            mGoToFileViewer = false;
        }

        public RefPathDisambiguationTask(String repoOwner, String repoName,
                String refAndPath, String fragment) {
            super();
            mRepoOwner = repoOwner;
            mRepoName = repoName;
            mRefAndPath = refAndPath;
            mFragment = fragment;
            mInitialPage = -1;
            mGoToFileViewer = true;
        }

        @Override
        protected Intent run() throws Exception {
            Pair<String, String> refAndPath = resolve();
            if (refAndPath == null) {
                return null;
            }

            if (mGoToFileViewer && refAndPath.second != null) {
                // parse line numbers from fragment
                int highlightStart = -1, highlightEnd = -1;
                // Line numbers are encoded either in the form #L12 or #L12-14
                if (mFragment != null && mFragment.startsWith("L")) {
                    try {
                        int dashPos = mFragment.indexOf("-L");
                        if (dashPos > 0) {
                            highlightStart = Integer.valueOf(mFragment.substring(1, dashPos));
                            highlightEnd = Integer.valueOf(mFragment.substring(dashPos + 2));
                        } else {
                            highlightStart = Integer.valueOf(mFragment.substring(1));
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }

                return FileViewerActivity.makeIntentWithHighlight(BrowseFilter.this,
                        mRepoOwner, mRepoName, refAndPath.first, refAndPath.second,
                        highlightStart, highlightEnd);
            } else if (!mGoToFileViewer) {
                return RepositoryActivity.makeIntent(BrowseFilter.this,
                        mRepoOwner, mRepoName, refAndPath.first, refAndPath.second, mInitialPage);
            }

            return null;
        }

        // returns ref, path
        private Pair<String, String> resolve() throws Exception {
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
            String potentialSha = slashPos > 0 ? mRefAndPath.substring(0, slashPos) : mRefAndPath;
            if (SHA1_PATTERN.matcher(potentialSha).matches()) {
                return Pair.create(potentialSha,
                        slashPos > 0 ? mRefAndPath.substring(slashPos + 1) : "");
            }

            return null;
        }
    }
}
