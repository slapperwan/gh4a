package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.PullRequest;
import com.meisolsson.githubsdk.model.PullRequestMarker;
import com.meisolsson.githubsdk.model.git.GitReference;
import com.meisolsson.githubsdk.service.git.GitService;

public class ReferenceLoader extends BaseLoader<GitReference> {
    private final PullRequest mPullRequest;

    public ReferenceLoader(Context context, PullRequest pullRequest) {
        super(context);
        mPullRequest = pullRequest;
    }

    @Override
    public GitReference doLoadInBackground() throws ApiRequestException {
        GitService service = Gh4Application.get().getGitHubService(GitService.class);

        PullRequestMarker head = mPullRequest.head();
        if (head.repo() == null) {
            return null;
        }
        String owner = head.repo().owner().login();
        String repo = head.repo().name();
        String ref = head.ref();

        return ApiHelpers.throwOnFailure(service.getGitReference(owner, repo, ref).blockingGet());
    }
}
