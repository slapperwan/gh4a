package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class StarredGistListLoader extends BaseLoader<List<Gist>> {
    public StarredGistListLoader(Context context) {
        super(context);
    }

    @Override
    public List<Gist> doLoadInBackground() throws IOException {
        GistService gistService = (GistService)
                Gh4Application.get().getService(Gh4Application.GIST_SERVICE);
        return gistService.getStarredGists();
    }
}
