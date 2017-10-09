package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.issues.IssueAssigneeService;

import java.util.List;

public class AssigneeListLoader extends BaseLoader<List<User>> {
    private final String mRepoOwner;
    private final String mRepoName;

    public AssigneeListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }

    @Override
    protected List<User> doLoadInBackground() throws ApiRequestException {
        final IssueAssigneeService service =
                Gh4Application.get().getGitHubService(IssueAssigneeService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getAssignees(mRepoOwner, mRepoName, page))
                .blockingGet();
    }
}
