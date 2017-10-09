package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.service.search.SearchService;

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
    public Integer doLoadInBackground() throws ApiRequestException {
        SearchService service = ServiceFactory.createService(SearchService.class, null, null, 1);
        String query = String.format(Locale.US, QUERY_FORMAT,
                mRepository.owner().login(), mRepository.name(), mState);
        SearchPage<Issue> page = ApiHelpers.throwOnFailure(
                service.searchIssues(query, null, null, 0).blockingGet());
        return page.totalCount();
    }
}
