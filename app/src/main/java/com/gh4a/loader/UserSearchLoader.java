package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.search.SearchService;

public class UserSearchLoader extends BaseLoader<List<User>> {
    private final String mQuery;

    public UserSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<User> doLoadInBackground() throws Exception {
        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        final SearchService service = Gh4Application.get().getGitHubService(SearchService.class);
        List<User> result = new ArrayList<>();
        int nextPage = 1;
        do {
            SearchPage<User> page = ApiHelpers.throwOnFailure(
                    service.searchUsers(mQuery, null, null, nextPage).blockingGet());
            result.addAll(page.items());
            nextPage = page.next() != null ? page.next() : 0;
        } while (nextPage > 0);
        return result;
    }
}
