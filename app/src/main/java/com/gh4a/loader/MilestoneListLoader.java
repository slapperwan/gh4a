package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.Milestone;
import com.meisolsson.githubsdk.service.issues.IssueMilestoneService;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MilestoneListLoader extends BaseLoader<List<Milestone>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final IssueState mState;

    public MilestoneListLoader(Context context, String repoOwner, String repoName, IssueState state) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mState = state;
    }

    @Override
    public List<Milestone> doLoadInBackground() throws ApiRequestException {
        final IssueMilestoneService service =
                Gh4Application.get().getGitHubService(IssueMilestoneService.class);
        final String state = mState == null ? "all" : mState == IssueState.Open ? "open" : "closed";
        List<Milestone> milestones = ApiHelpers.PageIterator
                .toSingle(page -> service.getRepositoryMilestones(mRepoOwner, mRepoName, state, page))
                .blockingGet();

        if (milestones != null && mState == null) {
            Collections.sort(milestones, new Comparator<Milestone>() {
                @Override
                public int compare(Milestone lhs, Milestone rhs) {
                    IssueState leftState = lhs.state();
                    IssueState rightState = rhs.state();
                    if (leftState == rightState) {
                        return lhs.title().compareToIgnoreCase(rhs.title());
                    } else if (leftState == IssueState.Closed) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            });
        }

        return milestones;
    }
}
