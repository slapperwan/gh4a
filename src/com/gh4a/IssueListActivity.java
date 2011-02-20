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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.IssueAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.schema.Issue.State;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;

/**
 * The IssueList activity.
 */
public class IssueListActivity extends BaseActivity implements OnItemClickListener {

    /** The state. */
    protected String mState;

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The issue adapter. */
    protected IssueAdapter mIssueAdapter;

    /** The list view issues. */
    protected ListView mListViewIssues;

    /** The row layout. */
    protected int mRowLayout;

    /** The tv subtitle. */
    protected TextView mTvSubtitle;

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
        setRequestData();
        setBreadCrumb();

        mListViewIssues = (ListView) findViewById(R.id.list_view);
        mListViewIssues.setOnItemClickListener(this);

        mIssueAdapter = new IssueAdapter(this, new ArrayList<Issue>());
        mListViewIssues.setAdapter(mIssueAdapter);

        new LoadIssueListTask(this).execute();
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.BaseActivity#setUpActionBar()
     */
    @Override
    public void setUpActionBar() {
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        if (isAuthenticated()) {
            Intent intent = new Intent().setClass(getApplicationContext(), UserActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, getAuthUsername());
            actionBar.setHomeAction(new IntentAction(this, intent, R.drawable.ic_home));
        }
        actionBar.addAction(new IntentAction(this, new Intent(getApplicationContext(),
                SearchActivity.class), R.drawable.ic_search));
        actionBar.addAction(new IntentAction(this, new Intent(getApplicationContext(),
                ChooseIssueStateDialog.class), R.drawable.ic_search));
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

        createBreadcrumb("Issues", breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load issue list.
     */
    private static class LoadIssueListTask extends AsyncTask<Void, Integer, List<Issue>> {

        /** The target. */
        private WeakReference<IssueListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public LoadIssueListTask(IssueListActivity activity) {
            mTarget = new WeakReference<IssueListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Issue> doInBackground(Void... params) {
            try {
                IssueListActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                IssueService service = factory.createIssueService();
                return service.getIssues(activity.mUserLogin, activity.mRepoName, State
                        .valueOf(activity.mState));
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
        protected void onPostExecute(List<Issue> result) {
            IssueListActivity activity = mTarget.get();
            activity.mLoadingDialog.dismiss();

            if (mException) {
                activity.showError();
            }
            else {
                activity.fillData(result);
            }
        }
    }

    /**
     * Sets the request data.
     */
    protected void setRequestData() {
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mState = data.getString(Constants.Issue.ISSUE_STATE);
    }

    /**
     * Fill data into UI components.
     * 
     * @param issues the issues
     */
    protected void fillData(List<Issue> issues) {
        if (issues != null && issues.size() > 0) {
            mIssueAdapter.notifyDataSetChanged();
            for (Issue issue : issues) {
                mIssueAdapter.add(issue);
            }
            ((TextView) findViewById(R.id.tv_subtitle)).setText("Issues (" + issues.size() + ")");
        }
        mIssueAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Issue issue = (Issue) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(IssueListActivity.this, IssueActivity.class);
        Bundle data = getApplicationContext().populateIssue(issue);
        // extra data
        data.putString(Constants.Repository.REPO_OWNER, mUserLogin);
        data.putString(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }
}