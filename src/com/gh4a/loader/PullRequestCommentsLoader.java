package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PullRequestCommentsLoader extends BaseLoader<List<CommitComment>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;

    public PullRequestCommentsLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<CommitComment> doLoadInBackground() throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        List<CommitComment> comments = pullRequestService.getComments(
                new RepositoryId(mRepoOwner, mRepoName), mPullRequestNumber);
        List<CommitComment> result = new ArrayList<>();
        for (CommitComment comment : comments) {
            if (comment.getPosition() >= 0) {
                result.add(comment);
            }
        }
        return result;
    }
}
