package com.gh4a.loader;

import java.io.IOException;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;

import android.content.Context;

import com.gh4a.Gh4Application;

//XXX: make me a task
public class EditCommentLoader extends BaseLoader<Comment> {

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
    public Comment doLoadInBackground() throws IOException {
        IssueService issueService = (IssueService)
                Gh4Application.get(getContext()).getService(Gh4Application.ISSUE_SERVICE);
        
        Comment comment = new Comment();
        comment.setBody(mBody);
        comment.setId(mCommentId);
        return issueService.editComment(new RepositoryId(mRepoOwner, mRepoName), comment);
    }
}
