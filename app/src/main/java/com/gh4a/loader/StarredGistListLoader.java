package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.model.Page;
import com.meisolsson.githubsdk.service.gists.GistService;

public class StarredGistListLoader extends BaseLoader<List<Gist>> {
    public StarredGistListLoader(Context context) {
        super(context);
    }

    @Override
    public List<Gist> doLoadInBackground() throws IOException {
        final GistService service = Gh4Application.get().getGitHubService(GistService.class);
        return ApiHelpers.Pager.fetchAllPages(new ApiHelpers.Pager.PageProvider<Gist>() {
            @Override
            public Page<Gist> providePage(long page) throws IOException {
                return ApiHelpers.throwOnFailure(service.getUserStarredGists(page).blockingGet());
            }
        });
    }
}
