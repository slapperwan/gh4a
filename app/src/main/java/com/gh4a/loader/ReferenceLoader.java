package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.Reference;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.DataService;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class ReferenceLoader extends BaseLoader<Reference> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;

    public ReferenceLoader(Context context, String repoOwner, String repoName,
            int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public Reference doLoadInBackground() throws IOException {
        PullRequestService pullRequestService =
                (PullRequestService) Gh4Application.get().getService(Gh4Application.PULL_SERVICE);

        PullRequest pullRequest = pullRequestService.getPullRequest(
                new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);

        DataService dataService = (DataService)
                Gh4Application.get().getService(Gh4Application.DATA_SERVICE);

        PullRequestMarker head = pullRequest.getHead();
        String owner = head.getRepo().getOwner().getLogin();
        String repo = head.getRepo().getName();
        String ref = "heads/" + head.getRef();

        return dataService.getReference(new RepositoryId(owner, repo), ref);
    }
}
