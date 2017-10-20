package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Pair;
import android.view.View;

import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.List;

import io.reactivex.Single;

public class PullRequestFilesFragment extends CommitFragment {
    public static PullRequestFilesFragment newInstance(String repoOwner, String repoName,
            int pullRequestNumber, String headSha) {
        PullRequestFilesFragment f = new PullRequestFilesFragment();

        Bundle args = new Bundle();
        args.putString("owner", repoOwner);
        args.putString("repo", repoName);
        args.putInt("number", pullRequestNumber);
        args.putString("head", headSha);
        f.setArguments(args);
        return f;
    }

    private static final int ID_LOADER_FILES_AND_COMMENTS = 0;
    private static final int REQUEST_DIFF_VIEWER = 1000;

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private String mHeadSha;
    private List<ReviewComment> mComments;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mRepoOwner = args.getString("owner");
        mRepoName = args.getString("repo");
        mPullRequestNumber = args.getInt("number");
        mHeadSha = args.getString("head");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContentView.findViewById(R.id.iv_gravatar).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_author).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_timestamp).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_title).setVisibility(View.GONE);
        mContentView.findViewById(R.id.iv_commit_gravatar).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_commit_extra).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_message).setVisibility(View.GONE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setContentShown(false);
        load(false);
    }

    @Override
    public void onRefresh() {
        mComments = null;
        setContentShown(false);
        load(true);
    }

    @Override
    protected void handleFileClick(GitHubFile file) {
        final Intent intent;
        if (FileUtils.isImage(file.filename())) {
            intent = FileViewerActivity.makeIntent(getActivity(),
                    mRepoOwner, mRepoName, mHeadSha, file.filename());
        } else {
            intent = PullRequestDiffViewerActivity.makeIntent(getActivity(),
                    mRepoOwner, mRepoName, mPullRequestNumber, mHeadSha, file.filename(),
                    file.patch(), mComments, -1, -1, -1, false, null);
        }

        startActivityForResult(intent, REQUEST_DIFF_VIEWER);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_DIFF_VIEWER) {
            if (resultCode == Activity.RESULT_OK) {
                // reload comments
                getLoaderManager().getLoader(1).onContentChanged();

                if (getActivity() instanceof CommentUpdateListener) {
                    CommentUpdateListener l = (CommentUpdateListener) getActivity();
                    l.onCommentsUpdated();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void load(boolean force) {
        final PullRequestService service =
                Gh4Application.get().getGitHubService(PullRequestService.class);
        final PullRequestReviewCommentService commentService =
                Gh4Application.get().getGitHubService(PullRequestReviewCommentService.class);

        Single<List<GitHubFile>> fileSingle = ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(
                        mRepoOwner, mRepoName, mPullRequestNumber, page));
        Single<List<ReviewComment>> commentSingle = ApiHelpers.PageIterator
                .toSingle(page -> commentService.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() >= 0));

        Single.zip(fileSingle, commentSingle, Pair::create)
                .compose(makeLoaderSingle(ID_LOADER_FILES_AND_COMMENTS, force))
                .subscribe(result -> {
                    mComments = result.second;
                    fillHeader();
                    fillStats(result.first, result.second);
                    setContentShown(true);
                }, error -> {
                });
    }
}