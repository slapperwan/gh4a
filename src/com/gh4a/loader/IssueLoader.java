package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class IssueLoader extends BaseLoader<Issue> {

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;

    public IssueLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public Issue doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        return issueService.getIssue(mRepoOwner, mRepoName, mIssueNumber);
    }
}
