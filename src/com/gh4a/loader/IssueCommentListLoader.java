package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IssueCommentListLoader extends BaseLoader<List<Comment>> {

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    
    public IssueCommentListLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public List<Comment> doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                getContext().getApplicationContext().getSystemService(Gh4Application.ISSUE_SERVICE);
        return issueService.getComments(new RepositoryId(mRepoOwner, mRepoName), mIssueNumber);
    }

}
