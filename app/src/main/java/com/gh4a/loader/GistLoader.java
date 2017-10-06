package com.gh4a.loader;

import java.io.IOException;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Gist;
import com.meisolsson.githubsdk.service.gists.GistService;

public class GistLoader extends BaseLoader<Gist> {
    private final String mGistId;

    public GistLoader(Context context, String gistId) {
        super(context);
        mGistId = gistId;
    }

    @Override
    public Gist doLoadInBackground() throws IOException {
        GistService service = Gh4Application.get().getGitHubService(GistService.class);
        return ApiHelpers.throwOnFailure(service.getGist(mGistId).blockingGet());
    }
}
