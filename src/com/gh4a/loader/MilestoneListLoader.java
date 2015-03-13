package com.gh4a.loader;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;

public class MilestoneListLoader extends BaseLoader<List<Milestone>> {
    private String mRepoOwner;
    private String mRepoName;
    private String mState;

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
        List<Milestone> milestones = milestoneService.getMilestones(mRepoOwner, mRepoName, null);

        if (milestones != null && mState == null) {
            Collections.sort(milestones, new Comparator<Milestone>() {
                @Override
                public int compare(Milestone lhs, Milestone rhs) {
                    String leftState = lhs.getState();
                    String rightState = rhs.getState();
                    if (TextUtils.equals(leftState, rightState)) {
                        return 0;
                    } else if (Constants.Issue.STATE_CLOSED.equals(leftState)) {
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
