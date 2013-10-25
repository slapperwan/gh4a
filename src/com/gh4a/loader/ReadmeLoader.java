package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;

import android.content.Context;

import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class ReadmeLoader extends BaseLoader<String> {

    private String mRepoOwner;
    private String mRepoName;
    
    public ReadmeLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public String doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient("application/vnd.github.beta.html");
        client.setOAuth2Token(app.getAuthToken());
        
        ContentsService contentService = new ContentsService(client);
        return contentService.getReadmeHtml(new RepositoryId(mRepoOwner, mRepoName));
    }
}
