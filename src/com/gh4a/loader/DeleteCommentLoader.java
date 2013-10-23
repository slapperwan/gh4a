package com.gh4a.loader;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class DeleteCommentLoader extends BaseLoader<Void> {

    private String mRepoOwner;
    private String mRepoName;
    private long mCommentId;
    
    public DeleteCommentLoader(Context context, String repoOwner, 
            String repoName, long commentId) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mCommentId = commentId;
    }
    
    @Override
    public Void doLoadInBackground() throws Exception {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        issueService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mCommentId);
        return null;
    }
}
