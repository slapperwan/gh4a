package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class TagListLoader extends BaseLoader<List<RepositoryTag>> {

    private String mRepoOwner;
    private String mRepoName;
    
    public TagListLoader(Context context, String repoOwner, String repoName) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
    }
    
    @Override
    public List<RepositoryTag> doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        RepositoryService repoService = new RepositoryService(client);
        return repoService.getTags(new RepositoryId(mRepoOwner, mRepoName));
    }
}
