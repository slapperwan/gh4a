package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class CommitCommentListLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    private String mSha;
    
    public CommitCommentListLoader(Context context, String repoOwner, String repoName, String sha) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
    }

    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(app.getAuthToken());
        CommitService commitService = new CommitService(client);
        result.put(LoaderResult.DATA, 
                commitService.getComments(new RepositoryId(mRepoOwner, mRepoName),mSha));
    }

}
