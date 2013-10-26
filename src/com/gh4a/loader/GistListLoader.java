package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.Gist;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class GistListLoader extends BaseLoader<List<Gist>> {
    private String mUserName;
    
    public GistListLoader(Context context, String userName) {
        super(context);
        mUserName = userName;
    }

    @Override
    public List<Gist> doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get(getContext());
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        GistService gistService = new GistService(client);
        return gistService.getGists(mUserName);
    }
}
