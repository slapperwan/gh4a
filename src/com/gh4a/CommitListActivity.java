/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.CommitAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Commit;
import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;

/**
 * The CommitList activity.
 */
public class CommitListActivity extends BaseActivity implements OnScrollListener,
        OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The branch. */
    protected String mBranchName;

    /** The tree sha. */
    protected String mTreeSha;

    /** The from btn id. */
    protected int mFromBtnId;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The commit adapter. */
    protected CommitAdapter mCommitAdapter;

    /** The list view commits. */
    protected ListView mListViewCommits;

    /** The loading. */
    protected boolean mLoading;

    /** The reload. */
    protected boolean mReload;

    /** The page. */
    protected int mPage = 1;

    /** The last page. */
    protected boolean lastPage;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        mListViewCommits = (ListView) findViewById(R.id.list_view);

        mListViewCommits.setOnItemClickListener(this);
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mBranchName = getIntent().getExtras().getString(Constants.Repository.REPO_BRANCH);
        mTreeSha = getIntent().getExtras().getString(Constants.Object.TREE_SHA);
        mFromBtnId = getIntent().getExtras().getInt(Constants.VIEW_ID);

        setBreadCrumb();

        mCommitAdapter = new CommitAdapter(this, new ArrayList<Commit>());
        mListViewCommits.setAdapter(mCommitAdapter);
        mListViewCommits.setOnScrollListener(this);

        new LoadCommitListTask(this).execute("false");
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[4];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);
        data.put(Constants.Repository.REPO_NAME, mRepoName);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        // Repo
        b = new BreadCrumbHolder();
        b.setLabel(mRepoName);
        b.setTag(Constants.Repository.REPO_NAME);
        b.setData(data);
        breadCrumbHolders[1] = b;

        // branches/tags
        b = new BreadCrumbHolder();
        if (R.id.btn_tags == mFromBtnId) {
            b.setLabel("tags");
            b.setTag(Constants.Object.TAGS);
        }
        else {
            b.setTag(Constants.Object.BRANCHES);
            b.setLabel("branches");
        }

        b.setData(data);
        breadCrumbHolders[2] = b;

        // branch name
        b = new BreadCrumbHolder();
        b.setLabel(mBranchName);
        b.setTag(Constants.Repository.REPO_BRANCH);
        data.put(Constants.Object.TREE_SHA, mTreeSha);
        data.put(Constants.Repository.REPO_BRANCH, mBranchName);
        data.put(Constants.Object.TREE_SHA, mTreeSha);
        data.put(Constants.VIEW_ID, String.valueOf(mFromBtnId));
        b.setData(data);
        breadCrumbHolders[3] = b;

        createBreadcrumb("Commits", breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load commit
     * list.
     */
    private static class LoadCommitListTask extends AsyncTask<String, Integer, List<Commit>> {

        /** The target. */
        private WeakReference<CommitListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        /** The hide main view. */
        private boolean mHideMainView;
        
        /** The last page. */
        private boolean mLastPage;

        /**
         * Instantiates a new load commit list task.
         *
         * @param activity the activity
         */
        public LoadCommitListTask(CommitListActivity activity) {
            mTarget = new WeakReference<CommitListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Commit> doInBackground(String... params) {
            if (!mTarget.get().lastPage) {
                this.mHideMainView = Boolean.valueOf(params[0]);
                CommitListActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                CommitService commitService = factory.createCommitService();
                try {
                    List<Commit> commits = commitService.getCommits(activity.mUserLogin,
                            activity.mRepoName, activity.mBranchName, activity.mPage);
                    activity.mPage++;
                    return commits;
                }
                catch (GitHubException e) {
                    if (!e.getMessage().contains("Not Found")) {
                        Log.e(Constants.LOG_TAG, e.getMessage(), e);
                        mException = true;
                        return null;
                    }
                    else {
                        mException = false;
                        activity.lastPage = true;
                        return null;
                    }
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get().mPage == 1) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                        mHideMainView);
            }
            else {
                TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                if (mTarget.get().lastPage) {
                    Toast.makeText(mTarget.get(), "No more result", Toast.LENGTH_SHORT).show();
                }
                else {
                    loadingView.setVisibility(View.VISIBLE);
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Commit> result) {
            CommitListActivity activity = mTarget.get();

            if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                activity.mLoadingDialog.dismiss();
            }

            TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
            loadingView.setVisibility(View.GONE);

            if (mException) {
                activity.showError();
            }
            else {
                if (result != null) {
                    activity.fillData(result);
                }
                activity.mLoading = false;
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param commits the commits
     */
    protected void fillData(List<Commit> commits) {
        if (commits != null && commits.size() > 0) {
            mCommitAdapter.notifyDataSetChanged();
            for (Commit commit : commits) {
                mCommitAdapter.add(commit);
            }
        }
        mCommitAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AbsListView.OnScrollListener#onScrollStateChanged(android
     * .widget.AbsListView, int)
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (mReload && scrollState == SCROLL_STATE_IDLE) {
            new LoadCommitListTask(this).execute("false");
            mReload = false;
        }
    }

    /*
     * (non-Javadoc)
     * @seeandroid.widget.AbsListView.OnScrollListener#onScroll(android.widget.
     * AbsListView, int, int, int)
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (!mLoading && firstVisibleItem != 0
                && ((firstVisibleItem + visibleItemCount) == totalItemCount)) {
            mReload = true;
            mLoading = true;
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Commit commit = (Commit) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(CommitListActivity.this, CommitActivity.class);
        String[] urlPart = commit.getUrl().split("/");

        intent.putExtra(Constants.Repository.REPO_OWNER, urlPart[1]);
        intent.putExtra(Constants.Repository.REPO_NAME, urlPart[2]);
        intent.putExtra(Constants.Object.OBJECT_SHA, urlPart[4]);

        startActivity(intent);
    }
}