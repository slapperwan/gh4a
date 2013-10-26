package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.GistService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class GistLoader extends BaseLoader<String> {
    private String mFileName;
    private String mGistId;
    
    public GistLoader(Context context, String fileName, String gistId) {
        super(context);
        mFileName = fileName;
        mGistId = gistId;
    }

    @Override
    public String doLoadInBackground() throws IOException {
        Gh4Application app = Gh4Application.get(getContext());
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        GistService gistService = new GistService(client);
        return gistService.getGist(mGistId).getFiles().get(mFileName).getContent();
    }
}
