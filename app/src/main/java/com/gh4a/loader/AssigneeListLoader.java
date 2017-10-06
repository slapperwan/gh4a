package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.issues.IssueAssigneeService;

import java.io.IOException;
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
    protected List<User> doLoadInBackground() throws Exception {
        final IssueAssigneeService service =
                Gh4Application.get().getGitHubService(IssueAssigneeService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<User>() {
            @Override
            public Page<User> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(
                        service.getAssignees(mRepoOwner, mRepoName, page).blockingGet());
            }
        });
    }
}
