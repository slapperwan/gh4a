package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.ContentService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.Gh4Application;

public class ContentListLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    private String mPath;
    private String mRef;
    
    public ContentListLoader(Context context, String repoOwner, String repoName, String path, String ref) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mPath = path;
        mRef = ref;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(app.getAuthToken());
        
        RepositoryService repoService = new RepositoryService(client);
        ContentService contentService = new ContentService(client);
        if (mRef == null) {
            Repository repo = repoService.getRepository(mRepoOwner, mRepoName);
            mRef = repo.getMasterBranch();
        }
        result.put(LoaderResult.DATA, 
                contentService.getContents(new RepositoryId(mRepoOwner, mRepoName), mPath, mRef));
    }

}
