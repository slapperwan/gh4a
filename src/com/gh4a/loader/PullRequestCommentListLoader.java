package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class PullRequestCommentListLoader extends IssueCommentListLoader {
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;

    public PullRequestCommentListLoader(Context context, String repoOwner,
            String repoName, int issueNumber) {
        super(context, repoOwner, repoName, issueNumber);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public List<IssueEventHolder> doLoadInBackground() throws IOException {
        // combine issue comments and pull request comments (to get comments on diff)
        List<IssueEventHolder> events = super.doLoadInBackground();

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        List<CommitComment> commitComments =
                pullRequestService.getComments(new RepositoryId(mRepoOwner, mRepoName), mIssueNumber);

        // only add comment that is not outdated
        for (CommitComment commitComment: commitComments) {
            if (commitComment.getPosition() != -1) {
                events.add(new IssueEventHolder(commitComment));
            }
        }

        Collections.sort(events, SORTER);

        return events;
    }
}
