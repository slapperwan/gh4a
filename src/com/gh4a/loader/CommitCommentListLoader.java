package com.gh4a.loader;

import java.io.IOException;
import java.util.List;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.CommitService;

import android.content.Context;

import com.gh4a.Gh4Application;

public class CommitCommentListLoader extends BaseLoader<List<CommitComment>> {

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
    public List<CommitComment> doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get(getContext()).getService(Gh4Application.COMMIT_SERVICE);
        return commitService.getComments(new RepositoryId(mRepoOwner, mRepoName),mSha);
    }

}
