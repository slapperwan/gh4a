package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.service.issues.IssueService;

public class IssueLoader extends BaseLoader<Issue> {

    private final String mRepoOwner;
    private final String mRepoName;
    private final int mIssueNumber;

    public IssueLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public Issue doLoadInBackground() throws ApiRequestException {
        IssueService service = Gh4Application.get().getGitHubService(IssueService.class);
        return ApiHelpers.throwOnFailure(service.getIssue(mRepoOwner, mRepoName, mIssueNumber).blockingGet());
    }
}
