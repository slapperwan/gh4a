package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestCommentsLoader;
import com.gh4a.loader.PullRequestFilesLoader;
import com.gh4a.utils.FileUtils;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitFile;

import java.util.ArrayList;
import java.util.List;

public class PullRequestFilesFragment extends CommitFragment {
    private static final int REQUEST_DIFF_VIEWER = 1000;

    public interface CommentUpdateListener {
        void onCommentsUpdated();
    }

    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private String mHeadSha;
    private List<CommitFile> mFiles;
    private List<CommitComment> mComments;

    private LoaderCallbacks<List<CommitFile>> mPullRequestFilesCallback = new LoaderCallbacks<List<CommitFile>>(this) {
        @Override
        protected Loader<LoaderResult<List<CommitFile>>> onCreateLoader() {
            return new PullRequestFilesLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(List<CommitFile> result) {
            mFiles = result;
            fillDataIfReady();
        }
    };

    private LoaderCallbacks<List<CommitComment>> mPullRequestCommentsCallback =
            new LoaderCallbacks<List<CommitComment>>(this) {
        @Override
        protected Loader<LoaderResult<List<CommitComment>>> onCreateLoader() {
            return new PullRequestCommentsLoader(getActivity(),
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        protected void onResultReady(List<CommitComment> result) {
            mComments = result;
            fillDataIfReady();
        }
    };

    public static PullRequestFilesFragment newInstance(String repoOwner, String repoName,
            int pullRequestNumber, String headSha) {
        PullRequestFilesFragment f = new PullRequestFilesFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putInt(Constants.PullRequest.NUMBER, pullRequestNumber);
        args.putString(Constants.Object.REF, headSha);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Bundle args = getArguments();
        mRepoOwner = args.getString(Constants.Repository.OWNER);
        mRepoName = args.getString(Constants.Repository.NAME);
        mPullRequestNumber = args.getInt(Constants.PullRequest.NUMBER);
        mHeadSha = args.getString(Constants.Object.REF);
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
    public void onRefresh() {
        mFiles = null;
        mComments = null;
        hideContentAndRestartLoaders(0, 1);
    }

    @Override
    protected void initLoader() {
        getLoaderManager().initLoader(0, null, mPullRequestFilesCallback);
        getLoaderManager().initLoader(1, null, mPullRequestCommentsCallback);
    }

    private void fillDataIfReady() {
        if (mComments != null && mFiles != null) {
            fillStats(mFiles, mComments);
            setContentShown(true);
        }
    }

    @Override
    public void onClick(View v) {
        CommitFile file = (CommitFile) v.getTag();

        Intent intent = new Intent(getActivity(), FileUtils.isImage(file.getFilename())
                ? FileViewerActivity.class : PullRequestDiffViewerActivity.class);

        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.PullRequest.NUMBER, mPullRequestNumber);
        intent.putExtra(Constants.Object.REF, mHeadSha);
        intent.putExtra(Constants.Object.OBJECT_SHA, mHeadSha);
        intent.putExtra(Constants.Commit.DIFF, file.getPatch());
        intent.putExtra(Constants.Commit.COMMENTS, new ArrayList<>(mComments));
        intent.putExtra(Constants.Object.PATH, file.getFilename());
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