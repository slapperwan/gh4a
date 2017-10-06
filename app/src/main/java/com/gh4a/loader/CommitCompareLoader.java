package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

public class CommitCompareLoader extends BaseLoader<List<Commit>> {
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
    public List<Commit> doLoadInBackground() throws IOException {
        RepositoryCommitService service =
                Gh4Application.get().getGitHubService(RepositoryCommitService.class);
        // first try using the actual SHA1s
        List<Commit> commits = getCommitsOrNullOn404(service, mBase, mHead);
        if (commits == null && mBaseLabel != null && mHeadLabel != null) {
            // We got a 404; likely the history of the base branch was rewritten. Try the labels.
            commits = getCommitsOrNullOn404(service, mBaseLabel, mHeadLabel);
        }
        if (commits == null) {
            // Bummer, at least one branch was deleted.
            // Can't do anything here, so return an empty list.
            commits = new ArrayList<>();
        }
        return commits;
    }

    private List<Commit> getCommitsOrNullOn404(RepositoryCommitService service,
            String base, String head) throws IOException {
        try {
            return ApiHelpers.throwOnFailure(
                    service.compareCommits(mRepoOwner, mRepoName, base, head).blockingGet()).commits();
        } catch (ApiRequestException e) {
            // Ignore error 404
            if (e.getStatus() != 404) {
                throw e;
            }
        }
        return null;
    }
}
