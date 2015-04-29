package com.gh4a.loader;

import java.io.IOException;
import java.util.ArrayList;
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
    private boolean mIncludePositional;
    private boolean mIncludeUnpositional;

    public CommitCommentListLoader(Context context, String repoOwner, String repoName,
            String sha, boolean includeUnpositional, boolean includePositional) {
        super(context);
        mRepoOwner = repoOwner;
        mRepoName = repoName;
        mSha = sha;
        mIncludePositional = includePositional;
        mIncludeUnpositional = includeUnpositional;
    }

    @Override
    public List<CommitComment> doLoadInBackground() throws IOException {
        CommitService commitService = (CommitService)
                Gh4Application.get().getService(Gh4Application.COMMIT_SERVICE);
        List<CommitComment> comments = commitService.getComments(
                new RepositoryId(mRepoOwner, mRepoName), mSha);

        if (comments == null || (mIncludePositional && mIncludeUnpositional)) {
            return comments;
        }
        ArrayList<CommitComment> result = new ArrayList<>();
        for (CommitComment comment : comments) {
            int pos = comment.getPosition();
            if ((pos < 0 && mIncludeUnpositional) || (pos >= 0 && mIncludePositional)) {
                result.add(comment);
            }
        }
        return result;
    }
}
