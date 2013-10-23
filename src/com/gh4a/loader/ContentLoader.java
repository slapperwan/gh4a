package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentsService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class ContentLoader extends BaseLoader<List<RepositoryContents>> {

    private String mRepoOwner;
    private String mRepoName;
    private String mPath;
    private String mRef;
    
    public ContentLoader(Context context, String repoOwner, String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }
    
    @Override
    public List<RepositoryContents> doLoadInBackground() throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        ContentsService contentService = new ContentsService(client);
        return contentService.getContents(new RepositoryId(mRepoOwner, mRepoName), mPath, mRef);
    }
}
