package com.gh4a.fragment;

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
    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private List<CommitFile> mFiles;
    private List<CommitComment> mComments;

    private LoaderCallbacks<List<CommitFile>> mPullRequestFilesCallback = new LoaderCallbacks<List<CommitFile>>() {
        @Override
        public Loader<LoaderResult<List<CommitFile>>> onCreateLoader(int id, Bundle args) {
            return new PullRequestFilesLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitFile>> result) {
            if (result.handleError(getActivity())) {
                setContentEmpty(true);
                setContentShown(true);
                return;
            }
            mFiles = result.getData();
            fillDataIfReady();
        }
    };

    private LoaderCallbacks<List<CommitComment>> mPullRequestCommentsCallback =
            new LoaderCallbacks<List<CommitComment>>() {
        @Override
        public Loader<LoaderResult<List<CommitComment>>> onCreateLoader(int id, Bundle args) {
            return new PullRequestCommentsLoader(getActivity(),
                    mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitComment>> result) {
            if (result.handleError(getActivity())) {
                setContentEmpty(true);
                setContentShown(true);
                return;
            }
            mComments = result.getData();
            fillDataIfReady();
        }
    };

    public static PullRequestFilesFragment newInstance(String repoOwner, String repoName, int pullRequestNumber) {
        PullRequestFilesFragment f = new PullRequestFilesFragment();

        Bundle args = new Bundle();
        args.putString(Constants.Repository.OWNER, repoOwner);
        args.putString(Constants.Repository.NAME, repoName);
        args.putInt(Constants.PullRequest.NUMBER, pullRequestNumber);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRepoOwner = getArguments().getString(Constants.Repository.OWNER);
        mRepoName = getArguments().getString(Constants.Repository.NAME);
        mPullRequestNumber = getArguments().getInt(Constants.PullRequest.NUMBER);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mContentView.findViewById(R.id.title).setVisibility(View.GONE);
        mContentView.findViewById(R.id.committer_info).setVisibility(View.GONE);
        mContentView.findViewById(R.id.tv_message).setVisibility(View.GONE);
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
        intent.putExtra(Constants.Object.OBJECT_SHA, file.getRawUrl().split("/")[6]);
        intent.putExtra(Constants.Commit.DIFF, file.getPatch());
        intent.putExtra(Constants.Commit.COMMENTS, new ArrayList<CommitComment>(mComments));
        intent.putExtra(Constants.Object.PATH, file.getFilename());
        startActivity(intent);
    }
}