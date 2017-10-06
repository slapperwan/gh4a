package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.view.View;

import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.loader.PullRequestFilesLoader;
import com.gh4a.utils.FileUtils;
import com.meisolsson.githubsdk.model.GitHubFile;
import com.meisolsson.githubsdk.model.ReviewComment;

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

    private static final int REQUEST_DIFF_VIEWER = 1000;

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private String mHeadSha;
    private List<GitHubFile> mFiles;
    private List<ReviewComment> mComments;

    private final LoaderCallbacks<List<GitHubFile>> mPullRequestFilesCallback = new LoaderCallbacks<List<GitHubFile>>(this) {
        @Override
        protected Loader<LoaderResult<List<GitHubFile>>> onCreateLoader() {
            return new PullRequestFilesLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(List<GitHubFile> result) {
            mFiles = result;
            populateViewIfReady();
        }
    };

    private final LoaderCallbacks<List<ReviewComment>> mPullRequestCommentsCallback =
            new LoaderCallbacks<List<ReviewComment>>(this) {
        @Override
        protected Loader<LoaderResult<List<ReviewComment>>> onCreateLoader() {
            return new PullRequestCommentsLoader(getActivity(),
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(List<ReviewComment> result) {
            mComments = result;
            populateViewIfReady();
        }
    };

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
        getLoaderManager().initLoader(0, null, mPullRequestFilesCallback);
        getLoaderManager().initLoader(1, null, mPullRequestCommentsCallback);
    }

    @Override
    public void onRefresh() {
        mFiles = null;
        mComments = null;
        hideContentAndRestartLoaders(0, 1);
    }

    @Override
    protected void populateViewIfReady() {
        if (mComments != null && mFiles != null) {
            fillStats(mFiles, mComments);
            setContentShown(true);
        }
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
}