package com.gh4a.loader;

import java.util.List;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.service.gists.GistService;

public class StarredGistListLoader extends BaseLoader<List<Gist>> {
    public StarredGistListLoader(Context context) {
        super(context);
    }

    @Override
    public List<Gist> doLoadInBackground() throws ApiRequestException {
        final GistService service = Gh4Application.get().getGitHubService(GistService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getUserStarredGists(page))
                .blockingGet();
    }
}
