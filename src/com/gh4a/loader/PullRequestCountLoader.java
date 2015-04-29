package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;

public class PullRequestCountLoader extends BaseLoader<Integer> {

    private Repository mRepository;
    private String mState;

    public PullRequestCountLoader(Context context, Repository repository, String state) {
        super(context);
        mRepository = repository;
        mState = state;
    }

    @Override
    public Integer doLoadInBackground() throws IOException {
        PullRequestService pullRequestService = (PullRequestService)
                Gh4Application.get().getService(Gh4Application.PULL_SERVICE);
        return pullRequestService.getPullRequests(mRepository, mState).size();
    }
}
