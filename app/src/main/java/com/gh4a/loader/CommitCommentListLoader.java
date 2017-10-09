package com.gh4a.loader;

import android.content.Context;

import com.gh4a.ApiRequestException;
import com.gh4a.Gh4Application;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class CommitCommentListLoader extends BaseLoader<List<GitComment>> {
    private final String mRepoOwner;
    private final String mRepoName;
    private final String mSha;
    private final boolean mIncludePositional;
    private final boolean mIncludeUnpositional;

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
    public List<GitComment> doLoadInBackground() throws ApiRequestException {
        List<GitComment> comments = loadComments(mRepoOwner, mRepoName, mSha).blockingGet();

        if (comments == null || (mIncludePositional && mIncludeUnpositional)) {
            return comments;
        }
        ArrayList<GitComment> result = new ArrayList<>();
        for (GitComment comment : comments) {
            int pos = comment.position();
            if ((pos < 0 && mIncludeUnpositional) || (pos >= 0 && mIncludePositional)) {
                result.add(comment);
            }
        }
        return result;
    }

    public static Single<List<GitComment>> loadComments(final String repoOwner,
            final String repoName, final String sha) throws ApiRequestException {
        final RepositoryCommentService service =
                Gh4Application.get().getGitHubService(RepositoryCommentService.class);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitComments(repoOwner, repoName, sha, page));
    }
}
