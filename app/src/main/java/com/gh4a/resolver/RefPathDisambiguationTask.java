package com.gh4a.resolver;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Pair;

import com.gh4a.Gh4Application;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.RepositoryActivity;

import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.util.List;
import java.util.regex.Pattern;

public class RefPathDisambiguationTask extends UrlLoadTask {
    private static final Pattern SHA1_PATTERN = Pattern.compile("[a-z0-9]{40}");

    @VisibleForTesting
    protected final String mRepoOwner;
    @VisibleForTesting
    protected final String mRepoName;
    @VisibleForTesting
    protected final String mRefAndPath;
    @VisibleForTesting
    protected final int mInitialPage;
    @VisibleForTesting
    protected final String mFragment;
    @VisibleForTesting
    protected final boolean mGoToFileViewer;

    public RefPathDisambiguationTask(FragmentActivity activity, String repoOwner,
            String repoName, String refAndPath, int initialPage) {
        super(activity);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mRefAndPath = refAndPath;
        mInitialPage = initialPage;
        mFragment = null;
        mGoToFileViewer = false;
    }

    public RefPathDisambiguationTask(FragmentActivity activity, String repoOwner,
            String repoName, String refAndPath, String fragment) {
        super(activity);
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

            return FileViewerActivity.makeIntentWithHighlight(mActivity,
                    mRepoOwner, mRepoName, refAndPath.first, refAndPath.second,
                    highlightStart, highlightEnd);
        } else if (!mGoToFileViewer) {
            return RepositoryActivity.makeIntent(mActivity,
                    mRepoOwner, mRepoName, refAndPath.first, refAndPath.second, mInitialPage);
        }

        return null;
    }

    // returns ref, path
    private Pair<String, String> resolve() throws Exception {
        // first check whether the path redirects to HEAD
        if (mRefAndPath.startsWith("HEAD")) {
            if (mRefAndPath.startsWith("HEAD/")) {
                return Pair.create("HEAD", mRefAndPath.substring(5));
            }
            return Pair.create("HEAD", null);
        }

        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        RepositoryId repo = new RepositoryId(mRepoOwner, mRepoName);

        // then look for matching branches
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

        if (mActivity.isFinishing()) {
            return null;
        }

        // and for tags after that
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
