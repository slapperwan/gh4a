package com.gh4a.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.devspark.progressfragment.SherlockProgressFragment;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.PullRequestDiffViewerActivity;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.PullRequestFilesLoader;

import org.eclipse.egit.github.core.CommitFile;

import java.util.List;

public class PullRequestFilesFragment extends SherlockProgressFragment implements OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private int mPullRequestNumber;
    private View mContentView;
    private List<CommitFile> mCommitFiles;

    private LoaderCallbacks<List<CommitFile>> mPullRequestFilesCallback = new LoaderCallbacks<List<CommitFile>>() {
        @Override
        public Loader<LoaderResult<List<CommitFile>>> onCreateLoader(int id, Bundle args) {
            return new PullRequestFilesLoader(getActivity(), mRepoOwner, mRepoName, mPullRequestNumber);
        }

        @Override
        public void onResultReady(LoaderResult<List<CommitFile>> result) {
            boolean success = !result.handleError(getActivity());
            if (success) {
                mCommitFiles = result.getData();
                fillData();
            }
            setContentEmpty(!success);
            setContentShown(true);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mContentView = inflater.inflate(R.layout.pull_request_files, null);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setContentView(mContentView);
        setContentShown(false);

        getLoaderManager().initLoader(0, null, mPullRequestFilesCallback);
    }

    private void fillData() {
        final Activity activity = getActivity();
        final Gh4Application app = Gh4Application.get(activity);
        final LayoutInflater inflater = getLayoutInflater(null);

        LinearLayout llChanged = (LinearLayout) mContentView.findViewById(R.id.ll_changed);
        LinearLayout llAdded = (LinearLayout) mContentView.findViewById(R.id.ll_added);
        LinearLayout llDeleted = (LinearLayout) mContentView.findViewById(R.id.ll_deleted);
        int added = 0, changed = 0, deleted = 0;
        int additions = 0, deletions = 0;

        int count = mCommitFiles != null ? mCommitFiles.size() : 0;
        for (int i = 0; i < count; i++) {
            CommitFile file = mCommitFiles.get(i);
            String status = file.getStatus();
            final LinearLayout parent;

            if ("added".equals(status)) {
                parent = llAdded;
                added++;
            } else if ("modified".equals(status) || "renamed".equals(status)) {
                parent = llChanged;
                changed++;
            } else if ("removed".equals(status)) {
                parent = llDeleted;
                deleted++;
            } else {
                continue;
            }

            additions += file.getAdditions();
            deletions += file.getDeletions();

            TextView fileNameView = (TextView) inflater.inflate(R.layout.commit_filename, parent, false);
            fileNameView.setText(file.getFilename());
            fileNameView.setTag(file);
            if (parent != llDeleted) {
                fileNameView.setTextColor(getResources().getColor(R.color.highlight));
                fileNameView.setOnClickListener(this);
            }
            parent.addView(fileNameView);
        }

        if (added == 0) {
            llAdded.setVisibility(View.GONE);
        } else {
            TextView tvAddedTitle = (TextView) mContentView.findViewById(R.id.commit_added);
            tvAddedTitle.setTypeface(app.boldCondensed);
        }
        if (changed == 0) {
            llChanged.setVisibility(View.GONE);
        } else {
            TextView tvChangeTitle = (TextView) mContentView.findViewById(R.id.commit_changed);
            tvChangeTitle.setTypeface(app.boldCondensed);
        }
        if (deleted == 0) {
            llDeleted.setVisibility(View.GONE);
        } else {
            TextView tvDeletedTitle = (TextView) mContentView.findViewById(R.id.commit_deleted);
            tvDeletedTitle.setTypeface(app.boldCondensed);
        }

        TextView tvSummary = (TextView) mContentView.findViewById(R.id.tv_desc);
        tvSummary.setText(getString(R.string.commit_summary, added + changed + deleted,
                additions, deletions));
    }

    @Override
    public void onClick(View v) {
        CommitFile file = (CommitFile) v.getTag();

        Intent intent = new Intent(getActivity(), PullRequestDiffViewerActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.putExtra(Constants.PullRequest.NUMBER, mPullRequestNumber);
        intent.putExtra(Constants.Object.OBJECT_SHA, file.getRawUrl().split("/")[6]);
        intent.putExtra(Constants.Commit.DIFF, file.getPatch());
        intent.putExtra(Constants.Object.PATH, file.getFilename());
        startActivity(intent);
    }
}