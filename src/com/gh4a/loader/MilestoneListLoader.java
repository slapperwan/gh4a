package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class MilestoneListLoader extends BaseLoader<List<Milestone>> {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    
    public MilestoneListLoader(Context context, String repoOwner, String repoName, String state) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mState = state;
    }
    
    @Override
    public List<Milestone> doLoadInBackground() throws IOException {
        MilestoneService milestoneService = (MilestoneService)
                Gh4Application.get(getContext()).getService(Gh4Application.MILESTONE_SERVICE);
        return milestoneService.getMilestones(mRepoOwner, mRepoName, mState);
    }
}
