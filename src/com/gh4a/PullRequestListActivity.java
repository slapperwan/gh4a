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

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.PullRequestAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.PullRequest;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.PullRequestService;

/**
 * The PullRequestList activity.
 */
public class PullRequestListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;
    
    /** The repo name. */
    protected String mRepoName;
    
    /** The pull request adapter. */
    protected PullRequestAdapter mPullRequestAdapter;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

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

        ListView listView = (ListView) findViewById(R.id.list_view);

        listView.setOnItemClickListener(this);

        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();

        mPullRequestAdapter = new PullRequestAdapter(this, new ArrayList<PullRequest>());
        listView.setAdapter(mPullRequestAdapter);

        new LoadPullRequestListTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[2];

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

        createBreadcrumb("Pull Requests", breadCrumbHolders);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        PullRequest pullRequest = (PullRequest) mPullRequestAdapter.getItem(position);
        getApplicationContext().openPullRequestActivity(PullRequestListActivity.this, mUserLogin,
                mRepoName, pullRequest.getNumber());
    }

    // private OnItemClickListener onItemClick = new OnItemClickListener() {
    //
    // @Override
    // public void onItemClick(AdapterView<?> adapterView, View view, int
    // position, long id) {
    // PullRequest pullRequest = (PullRequest)
    // mPullRequestAdapter.getItem(position);
    // getApplicationContext().openPullRequestActivity(PullRequestListActivity.this,
    // mUserLogin, mRepoName, pullRequest.getNumber());
    // }
    //        
    // };

    /**
     * An asynchronous task that runs on a background thread
     * to load pull request list.
     */
    private static class LoadPullRequestListTask extends
            AsyncTask<Void, Integer, List<PullRequest>> {

        /** The target. */
        private WeakReference<PullRequestListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load pull request list task.
         *
         * @param activity the activity
         */
        public LoadPullRequestListTask(PullRequestListActivity activity) {
            mTarget = new WeakReference<PullRequestListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<PullRequest> doInBackground(Void... params) {
            try {
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                PullRequestService pullRequestService = factory.createPullRequestService();
                return pullRequestService.getPullRequests(mTarget.get().mUserLogin,
                        mTarget.get().mRepoName);
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<PullRequest> result) {
            if (mException) {
                mTarget.get().showError();
            }
            else {
                mTarget.get().fillData(result);
                mTarget.get().mLoadingDialog.dismiss();
            }
        }
    }

    /**
     * Fill data into UI components.
     *
     * @param pullRequests the pull requests
     */
    protected void fillData(List<PullRequest> pullRequests) {
        if (pullRequests != null && !pullRequests.isEmpty()) {
            mPullRequestAdapter.notifyDataSetChanged();
            for (PullRequest pullRequest : pullRequests) {
                mPullRequestAdapter.add(pullRequest);
            }
            mPullRequestAdapter.notifyDataSetChanged();
        }
        else {
            getApplicationContext().notFoundMessage(this, "Pull Requests");
        }
    }
}
