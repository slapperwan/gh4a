package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IssueCommentsLoader extends BaseLoader<List<Comment>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;

    public IssueCommentsLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public List<Comment> doLoadInBackground() throws IOException {
        // combine issue comments and pull request comments (to get comments on diff)
        IssueService issueService = (IssueService)
                Gh4Application.get(getContext()).getService(Gh4Application.ISSUE_SERVICE);
        List<Comment> comments = issueService.getComments(mRepoOwner, mRepoName, mIssueNumber);

        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get(getContext()).getService(Gh4Application.PULL_SERVICE);
        List<CommitComment> commitComments =
                pullRequestService.getComments(new RepositoryId(mRepoOwner, mRepoName), mIssueNumber);

        // only add comment that is not outdated
        for (CommitComment commitComment: commitComments) {
            if (commitComment.getPosition() != -1) {
                comments.add(commitComment);
            }
        }

        Collections.sort(comments, new Comparator<Comment>() {

            @Override
            public int compare(Comment lhs, Comment rhs) {
                return lhs.getCreatedAt().compareTo(rhs.getCreatedAt());
            }

        });

        return comments;
    }
}
