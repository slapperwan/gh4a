package com.gh4a.loader;

import java.util.HashMap;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.DefaultClient;
import com.gh4a.Gh4Application;

public class EditCommentLoader extends BaseLoader {

    private String mRepoOwner;
    private String mRepoName;
    private long mCommentId;
    private String mBody;
    
    public EditCommentLoader(Context context, String repoOwner, 
            String repoName, long commentId, String body) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mCommentId = commentId;
        mBody = body;
    }
    
    @Override
    public void doLoadInBackground(HashMap<Integer, Object> result)
            throws Exception {
        Gh4Application app = (Gh4Application) getContext().getApplicationContext();
        GitHubClient client = new DefaultClient();
        client.setOAuth2Token(app.getAuthToken());
        IssueService issueService = new IssueService(client);
        
        Comment comment = new Comment();
        comment.setBody(mBody);
        comment.setId(mCommentId);
        issueService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }

}
