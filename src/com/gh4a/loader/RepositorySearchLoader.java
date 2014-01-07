package com.gh4a.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

public class RepositorySearchLoader extends BaseLoader<List<Repository>> {
    private String mUserLogin;
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
            return new ArrayList<Repository>();
        }

        RepositoryService repoService = (RepositoryService)
                Gh4Application.get(getContext()).getService(Gh4Application.REPO_SERVICE);
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("fork", "true");
        params.put("user", mUserLogin);

        return repoService.searchRepositories(mQuery, params);
    }
}
