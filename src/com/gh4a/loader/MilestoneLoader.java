package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class MilestoneLoader extends BaseLoader<Milestone> {

    private String mRepoOwner;
    private String mRepoName;
    private int mNumber;
    
    public MilestoneLoader(Context context, String repoOwner, String repoName, int number) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mNumber = number;
    }
    
    @Override
    public Milestone doLoadInBackground() throws IOException {
        MilestoneService milestoneService = (MilestoneService)
                Gh4Application.get(getContext()).getService(Gh4Application.MILESTONE_SERVICE);
        return milestoneService.getMilestone(mRepoOwner, mRepoName, mNumber);
    }
}
