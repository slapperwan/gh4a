package com.gh4a.loader;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Constants.LoaderResult;
import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class IssueCommentsLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;
    
    public IssueCommentsLoader(Context context, String repoOwner, String repoName, int issueNumber) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mIssueNumber = issueNumber;
    }

    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result) throws IOException {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        result.put(LoaderResult.DATA, issueService.getComments(mRepoOwner, mRepoName, mIssueNumber));
    }
}
