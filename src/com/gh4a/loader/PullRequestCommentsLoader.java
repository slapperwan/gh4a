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

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mPullRequestNumber;

    public PullRequestCommentsLoader(Context context, String repoOwner, String repoName, int pullRequestNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPullRequestNumber = pullRequestNumber;
    }

    @Override
    public List<CommitComment> doLoadInBackground() throws IOException {
        return loadComments(mRepoOwner, mRepoName, mPullRequestNumber);
    }

    public static List<CommitComment> loadComments(String repoOwner, String repoName,
            int pullRequestNumber) throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        List<CommitComment> comments = pullRequestService.getComments(
                new RepositoryId(repoOwner, repoName), pullRequestNumber);
        List<CommitComment> result = new ArrayList<>();
        for (CommitComment comment : comments) {
            if (comment.getPosition() >= 0) {
                result.add(comment);
            }
        }
        return result;
    }
}
