package com.gh4a.resolver;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.FragmentActivity;

import com.gh4a.ApiRequestException;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.CommitActivity;
import com.gh4a.activities.CommitDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.meisolsson.githubsdk.model.Commit;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.git.GitComment;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommentService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCommitService;

import java.util.List;

import io.reactivex.Single;

public class CommitDiffLoadTask extends DiffLoadTask<GitComment> {
    @VisibleForTesting
    protected final String mSha;

    public CommitDiffLoadTask(FragmentActivity activity, Uri urlToResolve,
            String repoOwner, String repoName, DiffHighlightId diffId, String sha) {
        super(activity, urlToResolve, repoOwner, repoName, diffId);
        mSha = sha;
    }

    @Override
    protected @NonNull Intent getLaunchIntent(String sha, @NonNull GitHubFile file,
            List<GitComment> comments, DiffHighlightId diffId) {
        return CommitDiffViewerActivity.makeIntent(mActivity, mRepoOwner, mRepoName,
                sha, file.filename(), file.patch(), comments, diffId.startLine,
                diffId.endLine, diffId.right, null);
    }

    @Override
    protected @NonNull Intent getFallbackIntent(String sha) {
        return CommitActivity.makeIntent(mActivity, mRepoOwner, mRepoName, sha);
    }

    @Override
    public Single<String> getSha() {
        return Single.just(mSha);
    }

    @Override
    protected Single<List<GitHubFile>> getFiles() throws ApiRequestException {
        RepositoryCommitService service = ServiceFactory.get(RepositoryCommitService.class, false);
        return service.getCommit(mRepoOwner, mRepoName, mSha)
                .map(ApiHelpers::throwOnFailure)
                .map(Commit::files);
    }

    @Override
    protected Single<List<GitComment>> getComments() throws ApiRequestException {
        final RepositoryCommentService service =
                ServiceFactory.get(RepositoryCommentService.class, false);
        return ApiHelpers.PageIterator
                .toSingle(page -> service.getCommitComments(mRepoOwner, mRepoName, mSha, page));
    }
}
