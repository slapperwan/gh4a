package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class GistLoader extends BaseLoader<Gist> {
    private String mGistId;

    public GistLoader(Context context, String gistId) {
        super(context);
        mGistId = gistId;
    }

    @Override
    public Gist doLoadInBackground() throws IOException {
        GistService gistService = (GistService)
                Gh4Application.get().getService(Gh4Application.GIST_SERVICE);
        return gistService.getGist(mGistId);
    }
}
