package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.service.gists.GistService;

import java.io.IOException;

public class GistStarLoader extends BaseLoader<Boolean> {
    private final String mGistId;

    public GistStarLoader(Context context, String id) {
        super(context);
        mGistId = id;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        GistService service = Gh4Application.get().getGitHubService(GistService.class);
        return ApiHelpers.throwOnFailure(service.checkIfGistIsStarred(mGistId).blockingGet());
    }
}
