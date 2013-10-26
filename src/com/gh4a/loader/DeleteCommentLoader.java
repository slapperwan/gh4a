package com.gh4a.loader;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Gh4Application;

//XXX: make me a task
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
        IssueService issueService = (IssueService)
                getContext().getApplicationContext().getSystemService(Gh4Application.ISSUE_SERVICE);
        issueService.deleteComment(new RepositoryId(mRepoOwner, mRepoName), mCommentId);
        return null;
    }
}
