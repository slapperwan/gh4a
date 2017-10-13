package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.search.SearchService;

public class UserSearchLoader extends BaseLoader<List<User>> {
    private final String mQuery;

    public UserSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<User> doLoadInBackground() throws ApiRequestException {
        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        final SearchService service = Gh4Application.get().getGitHubService(SearchService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.searchUsers(mQuery, null, null, page)
                        .compose(RxUtils::searchPageAdapter))
                .blockingGet();
    }
}
