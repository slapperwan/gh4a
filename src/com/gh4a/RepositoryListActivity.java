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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Repository;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.gh4a.adapter.RepositoryAdapter;

/**
 * The RepositoryList activity.
 */
public class RepositoryListActivity extends BaseActivity implements OnScrollListener,
        OnItemClickListener {

    /** The search key. */
    protected String mSearchKey;

    /** The title bar. */
    protected String mTitleBar;

    /** The sub title. */
    protected String mSubtitle;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The repository adapter. */
    protected RepositoryAdapter mRepositoryAdapter;

    /** The list view repos. */
    protected ListView mListViewRepos;

    /** The reloading. */
    protected boolean mReloading;

    /** The reload. */
    protected boolean mReload;

    /** The page. */
    protected int mPage = 1;

    /** The row layout. */
    protected int mRowLayout;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);

        setRequestData();
        setTitleBar();
        setSubtitle();
        setRowLayout();
        setUpActionBar();
        setBreadCrumbs();

        mListViewRepos = (ListView) findViewById(R.id.list_view);
        mListViewRepos.setOnItemClickListener(this);

        mRepositoryAdapter = new RepositoryAdapter(this, new ArrayList<Repository>(), mRowLayout);
        mListViewRepos.setAdapter(mRepositoryAdapter);
        mListViewRepos.setOnScrollListener(this);

        new LoadRepositoryListTask(this).execute(true);
    }

    /**
     * Sets the request data.
     */
    protected void setRequestData() {
        mSearchKey = getIntent().getExtras().getString("searchKey");
    }

    /**
     * Sets the title bar.
     */
    protected void setTitleBar() {
        mTitleBar = "Repositories";// default
    }

    /**
     * Sets the subtitle.
     */
    protected void setSubtitle() {
        mSubtitle = "Repositories";// default
    }

    /**
     * Sets the row layout.
     */
    protected void setRowLayout() {
        mRowLayout = R.layout.row_simple_3;// default
    }

    /**
     * Sets the bread crumbs.
     */
    protected void setBreadCrumbs() {
        // no breadcrumbs
    }

    /**
     * An asynchronous task that runs on a background thread to load repository
     * list.
     */
    private static class LoadRepositoryListTask extends
            AsyncTask<Boolean, Integer, List<Repository>> {

        /** The target. */
        private WeakReference<RepositoryListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load repository list task.
         *
         * @param activity the activity
         */
        public LoadRepositoryListTask(RepositoryListActivity activity) {
            mTarget = new WeakReference<RepositoryListActivity>(activity);
        }

        /** The hide main view. */
        private boolean hideMainView;

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Repository> doInBackground(Boolean... params) {
            if (mTarget.get() != null) {
                try {
                    this.hideMainView = params[0];
                    return mTarget.get().getRepositories();
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                        hideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Repository> result) {
            if (mTarget.get() != null) {
                RepositoryListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                    activity.mReloading = false;
                    activity.mPage++;
                }
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param repositories the repositories
     */
    protected void fillData(List<Repository> repositories) {
        if (repositories != null && repositories.size() > 0) {
            mRepositoryAdapter.notifyDataSetChanged();
            for (Repository repository : repositories) {
                mRepositoryAdapter.add(repository);
            }
        }
        mRepositoryAdapter.notifyDataSetChanged();
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
            new LoadRepositoryListTask(this).execute(false);
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
        if (!mReloading && firstVisibleItem != 0
                && ((firstVisibleItem + visibleItemCount) == totalItemCount)) {
            mReload = true;
            mReloading = true;
        }
    }

    /**
     * Gets the repositories.
     *
     * @return the repositories
     * @throws IOException 
     */
    protected List<Repository> getRepositories() throws IOException {
//        GitHubClient client = new GitHubClient();
//        client.setOAuth2Token(getAuthToken());
//        RepositoryService repoService = new RepositoryService(client);
//        return repoService.searchRepositories(mSearchKey);
        return new ArrayList<Repository>();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Repository repository = (Repository) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent()
                .setClass(RepositoryListActivity.this, RepositoryActivity.class);

        Bundle data = getApplicationContext().populateRepository(repository);

        intent.putExtra(Constants.DATA_BUNDLE, data);
        startActivity(intent);
    }
}