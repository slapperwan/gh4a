package com.gh4a.loader;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;
import android.text.TextUtils;

import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class CodeSearchLoader extends BaseLoader<List<CodeSearchResult>> {
    private final String mQuery;

    public CodeSearchLoader(Context context, String query) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<CodeSearchResult> doLoadInBackground() throws Exception {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient("application/vnd.github.v3.text-match+json");
        client.setOAuth2Token(app.getAuthToken());

        if (TextUtils.isEmpty(mQuery)) {
            return new ArrayList<>();
        }

        RepositoryService repoService = new RepositoryService(client);
        return repoService.searchCode(mQuery);
    }
}
