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
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.JobAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.schema.Job;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.JobService;

public class JobListActivity extends BaseActivity {
    
    private int page = 0;
    private LoadingDialog mLoadingDialog;
    private boolean mLoading;
    private boolean mReload;
    private ListView mListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();

        setBreadCrumb();
        
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnScrollListener(new JobScrollListener(this));
        JobAdapter adapter = new JobAdapter(this, new ArrayList<Job>());
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Job job = (Job) adapterView.getAdapter().getItem(position);
                Intent intent = new Intent().setClass(JobListActivity.this, JobActivity.class);
                intent.putExtra(Constants.Job.COMPANY, job.getCompany());
                intent.putExtra(Constants.Job.LOCATION, job.getLocation());
                intent.putExtra(Constants.Job.COMPANY_URL, job.getCompanyUrl());
                intent.putExtra(Constants.Job.TITLE, job.getTitle());
                intent.putExtra(Constants.Job.URL, job.getUrl());
                intent.putExtra(Constants.Job.ID, job.getId());
                intent.putExtra(Constants.Job.COMPANY_LOGO, job.getCompanyLogo());
                intent.putExtra(Constants.Job.TYPE, job.getType().value());
                intent.putExtra(Constants.Job.DESCRIPTION, job.getDescription());
                intent.putExtra(Constants.Job.HOW_TO_APPLY, job.getHowToApply());
                startActivity(intent);
            }
        });
        
        new LoadAllJobsTask(this, true).execute();
    }

    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(getResources().getString(R.string.explore));
        b.setTag(Constants.EXPLORE);
        breadCrumbHolders[0] = b;
        
        createBreadcrumb(getResources().getString(R.string.jobs), breadCrumbHolders);
    }
    
    private static class LoadAllJobsTask extends
            AsyncTask<String, Void, List<Job>> {

        /** The target. */
        private WeakReference<JobListActivity> mTarget;

        /** The exception. */
        private boolean mException;

        /** The hide main view. */
        private boolean mHideMainView;
        
        /**
         * Instantiates a new load tree list task.
         * 
         * @param activity the activity
         */
        public LoadAllJobsTask(JobListActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<JobListActivity>(activity);
            mHideMainView = hideMainView;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Job> doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                    JobService jobService = factory.createJobService();
                    return jobService.searchJobs(null);//search all
                }
                catch (Exception e) {
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
                if (mTarget.get().page == 0) {
                    mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                            mHideMainView);
                }
                else {
                    TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                    loadingView.setVisibility(View.VISIBLE);
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Job> result) {
            if (mTarget.get() != null) {
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    if (mTarget.get().mLoadingDialog != null && mTarget.get().mLoadingDialog.isShowing()) {
                        mTarget.get().mLoadingDialog.dismiss();
                    }
        
                    TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                    loadingView.setVisibility(View.GONE);
                    mTarget.get().fillData(result);
                    mTarget.get().page++;
                    mTarget.get().mLoading = false;
                }
            }
        }
    }

    private void fillData(List<Job> result) {
        if (result != null) {
            List<Job> jobs = ((JobAdapter) mListView.getAdapter()).getObjects();
            jobs.addAll(result);
            ((JobAdapter) mListView.getAdapter()).notifyDataSetChanged();
        }
    }
    
    private static class JobScrollListener implements OnScrollListener {

        /** The target. */
        private WeakReference<JobListActivity> mTarget;

        /**
         * Instantiates a new repository scoll listener.
         *
         * @param activity the activity
         * @param searchKey the search key
         * @param language the language
         */
        public JobScrollListener(JobListActivity activity) {
            super();
            mTarget = new WeakReference<JobListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScrollStateChanged(
         * android.widget.AbsListView, int)
         */
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mTarget.get() != null) {
                if (mTarget.get().mReload && scrollState == SCROLL_STATE_IDLE) {
                    new LoadAllJobsTask(mTarget.get(), false).execute();
                    mTarget.get().mReload = false;
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScroll(android.widget
         * .AbsListView, int, int, int)
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if (mTarget.get() != null) {
                if (!mTarget.get().mLoading && firstVisibleItem != 0
                        && ((firstVisibleItem + visibleItemCount) == totalItemCount)) {
                    mTarget.get().mReload = true;
                    mTarget.get().mLoading = true;
                }
            }
        }
    }
}
