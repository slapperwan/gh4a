package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.service.search.SearchService;

public class RepositorySearchLoader extends BaseLoader<List<Repository>> {
    private final String mUserLogin;
    private String mQuery;

    public RepositorySearchLoader(Context context, String userLogin) {
        super(context);
        mUserLogin = userLogin;
    }

    public void setQuery(String query) {
        mQuery = query;
        cancelLoad();
    }

    @Override
    public List<Repository> doLoadInBackground() throws Exception {
        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        SearchService service = Gh4Application.get().getGitHubService(SearchService.class);
        StringBuilder params = new StringBuilder(mQuery);
        params.append(" fork:true");
        if (mUserLogin != null) {
            params.append(" user:").append(mUserLogin);
        }

        List<Repository> result = new ArrayList<>();

        try {
            int nextPage = 1;
            do {
                SearchPage<Repository> page = ApiHelpers.throwOnFailure(
                        service.searchRepositories(params.toString(), null, null, nextPage).blockingGet());
                result.addAll(page.items());
                nextPage = page.next() != null ? page.next() : 0;
            } while (nextPage > 0);
        } catch (ApiRequestException e) {
            if (e.getStatus() == 422) {
                // With that status code, Github wants to tell us there are no
                // repositories to search in. Just pretend no error and return
                // an empty list in that case.
                result = new ArrayList<>();
            } else {
                throw e;
            }
        }

        return result;
    }
}