package com.gh4a.loader;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.service.MilestoneService;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MilestoneListLoader extends BaseLoader<List<Milestone>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mState;

    public MilestoneListLoader(Context context, String repoOwner, String repoName) {
        this(context, repoOwner, repoName, null);
    }

    public MilestoneListLoader(Context context, String repoOwner, String repoName, String state) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mState = state;
    }

    @Override
    public List<Milestone> doLoadInBackground() throws IOException {
        MilestoneService milestoneService = (MilestoneService)
                Gh4Application.get().getService(Gh4Application.MILESTONE_SERVICE);
        List<Milestone> milestones = milestoneService.getMilestones(mRepoOwner, mRepoName, mState);

        if (milestones != null && mState == null) {
            Collections.sort(milestones, new Comparator<Milestone>() {
                @Override
                public int compare(Milestone lhs, Milestone rhs) {
                    String leftState = lhs.getState();
                    String rightState = rhs.getState();
                    if (TextUtils.equals(leftState, rightState)) {
                        return lhs.getTitle().compareToIgnoreCase(rhs.getTitle());
                    } else if (ApiHelpers.IssueState.CLOSED.equals(leftState)) {
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
