package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.Gh4Application;

public class CodeSearchLoader extends BaseLoader<List<CodeSearchResult>> {
    private String mQuery;

    public CodeSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<CodeSearchResult> doLoadInBackground() throws Exception {
        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        RepositoryService repoService = (RepositoryService)
                Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
        return repoService.searchCode(mQuery);
    }
}
