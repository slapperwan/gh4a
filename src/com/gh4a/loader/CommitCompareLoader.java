package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryCommitCompare;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CommitCompareLoader extends BaseLoader<List<RepositoryCommit>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mBase;
    private final String mBaseLabel;
    private final String mHead;
    private final String mHeadLabel;

    public CommitCompareLoader(Context context, String repoOwner, String repoName,
            String baseLabel, String base, String headLabel, String head) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mBase = base;
        mBaseLabel = baseLabel;
        mHead = head;
        mHeadLabel = headLabel;
    }

    @Override
    public List<RepositoryCommit> doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
        // first try using the actual SHA1s
        List<RepositoryCommit> commits = getCommitsOrNullOn404(commitService, repoId, mBase, mHead);
        if (commits == null && mBaseLabel != null && mHeadLabel != null) {
            // We got a 404; likely the history of the base branch was rewritten. Try the labels.
            commits = getCommitsOrNullOn404(commitService, repoId, mBaseLabel, mHeadLabel);
        }
        if (commits == null) {
            // Bummer, at least one branch was deleted.
            // Can't do anything here, so return an empty list.
            commits = new ArrayList<>();
        }
        return commits;
    }

    private List<RepositoryCommit> getCommitsOrNullOn404(CommitService service,
            RepositoryId repoId, String base, String head) throws IOException {
        try {
            RepositoryCommitCompare compare = service.compare(repoId, base, head);
            return compare.getCommits();
        } catch (RequestException e) {
            // Ignore error 404
            if (e.getStatus() != 404) {
                throw e;
            }
        }
        return null;
    }
}
