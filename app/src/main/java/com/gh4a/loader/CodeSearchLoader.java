package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.ApiRequestException;
import com.gh4a.ServiceFactory;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.SearchCode;
import com.meisolsson.githubsdk.model.SearchPage;
import com.meisolsson.githubsdk.service.search.SearchService;

public class CodeSearchLoader extends BaseLoader<List<SearchCode>> {
    private final String mQuery;

    public CodeSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<SearchCode> doLoadInBackground() throws ApiRequestException {
        SearchService service = ServiceFactory.createService(SearchService.class,
                "application/vnd.github.v3.text-match+json", null, null);

        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        List<SearchCode> result = new ArrayList<>();
        int nextPage = 1;
        do {
            SearchPage<SearchCode> page = ApiHelpers.throwOnFailure(
                    service.searchCode(mQuery, null, null, nextPage).blockingGet());
            result.addAll(page.items());
            nextPage = page.next() != null ? page.next() : 0;
        } while (nextPage > 0);
        return result;
    }
}
