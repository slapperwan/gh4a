package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import android.view.View;

import com.gh4a.R;
import com.gh4a.ServiceFactory;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.utils.ActivityResultHelpers;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.FileUtils;
import com.gh4a.utils.RxUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.ReviewComment;
import com.meisolsson.githubsdk.service.pull_request.PullRequestReviewCommentService;
import com.meisolsson.githubsdk.service.pull_request.PullRequestService;

import java.util.List;

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

    private static final int ID_LOADER_FILES = 0;
    private static final int ID_LOADER_COMMENTS = 1;

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private String mHeadSha;
    private List<GitHubFile> mFiles;
    private List<ReviewComment> mComments;

    private final ActivityResultLauncher<Intent> mDiffViewerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultHelpers.ActivityResultSuccessCallback(() -> {
                // reload comments
                loadComments(true);

                if (getActivity() instanceof CommentUpdateListener) {
                    CommentUpdateListener l = (CommentUpdateListener) getActivity();
                    l.onCommentsUpdated();
                }
            })
    );

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

        setContentShown(false);
        loadFiles(false);
        loadComments(false);
    }

    @Override
    protected void populateUiIfReady() {
        if (mFiles != null && mComments != null) {
            fillStats(mFiles, mComments);
            setContentShown(true);
        }
    }

    @Override
    public void onRefresh() {
        mComments = null;
        setContentShown(false);
        loadFiles(true);
        loadComments(true);
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
        mDiffViewerLauncher.launch(intent);
    }

    private void loadFiles(boolean force) {
        final PullRequestService service = ServiceFactory.get(PullRequestService.class, force);
        ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestFiles(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(makeLoaderSingle(ID_LOADER_FILES, force))
                .subscribe(result -> {
                    mFiles = result;
                    populateUiIfReady();
                }, this::handleLoadFailure);
    }

    private void loadComments(boolean force) {
        final PullRequestReviewCommentService service =
                ServiceFactory.get(PullRequestReviewCommentService.class, force);
        ApiHelpers.PageIterator
                .toSingle(page -> service.getPullRequestComments(
                        mRepoOwner, mRepoName, mPullRequestNumber, page))
                .compose(RxUtils.filter(c -> c.position() != null && c.position() >= 0))
                .compose(makeLoaderSingle(ID_LOADER_COMMENTS, force))
                .subscribe(result -> {
                    mComments = result;
                    populateUiIfReady();
                }, this::handleLoadFailure);
    }
}