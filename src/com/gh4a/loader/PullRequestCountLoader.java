package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class PullRequestCountLoader extends BaseLoader<Integer> {

    private final Repository mRepository;
    private final String mState;

    private static final String QUERY_FORMAT = "type:pr repo:%s/%s state:%s";

    public PullRequestCountLoader(Context context, Repository repository, String state) {
        super(context);
        mRepository = repository;
        mState = state;
    }

    @Override
    public Integer doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);
        HashMap<String, String> filterData = new HashMap<>();
        filterData.put("q", String.format(Locale.US, QUERY_FORMAT,
                mRepository.getOwner().getLogin(), mRepository.getName(), mState));
        return issueService.getSearchIssueResultCount(filterData);
    }
}
