package com.gh4a.loader;

import android.content.Context;

import com.gh4a.Gh4Application;

import org.eclipse.egit.github.core.service.GistService;

import java.io.IOException;

public class GistStarLoader extends BaseLoader<Boolean> {
    private String mGistId;

    public GistStarLoader(Context context, String id) {
        super(context);
        mGistId = id;
    }

    @Override
    public Boolean doLoadInBackground() throws IOException {
        GistService gistService = (GistService)
                Gh4Application.get().getService(Gh4Application.GIST_SERVICE);
        return gistService.isStarred(mGistId);
    }
}
