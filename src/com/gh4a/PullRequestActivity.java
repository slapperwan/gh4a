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
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.PullRequestDiscussionAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.ImageDownloader;
import com.github.api.v2.schema.Discussion;
import com.github.api.v2.schema.PullRequest;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.PullRequestService;

/**
 * The PullRequest activity.
 */
public class PullRequestActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The repo name. */
    protected String mRepoName;

    /** The pull request number. */
    protected int mPullRequestNumber;

    /** The header. */
    protected LinearLayout mHeader;

    /** The pull request adapter. */
    protected PullRequestDiscussionAdapter mPullRequestDiscussionAdapter;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The discussion loaded. */
    protected boolean mDiscussionLoaded;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.pull_request);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mPullRequestNumber = data.getInt(Constants.PullRequest.PULL_REQUEST_NUMBER);

        setBreadCrumb();

        new LoadPullRequestTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[3];

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

        // Pull requests
        b = new BreadCrumbHolder();
        b.setLabel("Pull Requests");
        b.setTag(Constants.PullRequest.PULL_REQUESTS);
        b.setData(data);
        breadCrumbHolders[2] = b;

        createBreadcrumb("Pull Request", breadCrumbHolders);
    }

    /**
     * Fill data into UI components.
     * 
     * @param pullRequest the pull request
     */
    protected void fillData(final PullRequest pullRequest) {
        ListView lvComments = (ListView) findViewById(R.id.lv_comments);

        // set details inside listview header
        LayoutInflater infalter = getLayoutInflater();
        mHeader = (LinearLayout) infalter.inflate(R.layout.pull_request_header, lvComments, false);

        lvComments.addHeaderView(mHeader, null, true);

        List<Discussion> discussions = new ArrayList<Discussion>();
        mPullRequestDiscussionAdapter = new PullRequestDiscussionAdapter(PullRequestActivity.this,
                discussions, mRepoName, pullRequest.getIssueUser().getLogin());
        lvComments.setAdapter(mPullRequestDiscussionAdapter);

        ImageView ivGravatar = (ImageView) mHeader.findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(pullRequest.getIssueUser().getGravatarId(),
                ivGravatar);
        ivGravatar.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                getApplicationContext()
                        .openUserInfoActivity(PullRequestActivity.this,
                                pullRequest.getIssueUser().getLogin(),
                                pullRequest.getIssueUser().getName());
            }
        });
        TextView tvLogin = (TextView) mHeader.findViewById(R.id.tv_login);
        TextView tvCreateAt = (TextView) mHeader.findViewById(R.id.tv_created_at);
        TextView tvTitle = (TextView) mHeader.findViewById(R.id.tv_title);
        TextView tvDesc = (TextView) mHeader.findViewById(R.id.tv_desc);
        Button btnComments = (Button) mHeader.findViewById(R.id.btn_comments);

        tvLogin.setText(pullRequest.getIssueUser().getLogin());
        tvCreateAt.setText(pt.format(pullRequest.getIssueCreatedAt()));
        tvTitle.setText(pullRequest.getTitle());
        tvDesc.setText(pullRequest.getBody());
        btnComments.setText(String.valueOf(pullRequest.getComments()));
        btnComments.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!mDiscussionLoaded) {
                    fillDiscussion(pullRequest.getDiscussion());
                    mDiscussionLoaded = true;
                }
            }
        });
    }

    /**
     * Fill comment into UI components.
     * 
     * @param discussions the discussions
     */
    protected void fillDiscussion(List<Discussion> discussions) {
        if (discussions != null && discussions.size() > 0) {
            mPullRequestDiscussionAdapter.notifyDataSetChanged();
            for (Discussion discussion : discussions) {
                mPullRequestDiscussionAdapter.add(discussion);
            }
        }
        mPullRequestDiscussionAdapter.notifyDataSetChanged();
        mDiscussionLoaded = true;
    }

    /**
     * An asynchronous task that runs on a background thread to load pull
     * request.
     */
    private static class LoadPullRequestTask extends AsyncTask<Void, Integer, PullRequest> {

        /** The target. */
        private WeakReference<PullRequestActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load pull request task.
         *
         * @param activity the activity
         */
        public LoadPullRequestTask(PullRequestActivity activity) {
            mTarget = new WeakReference<PullRequestActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected PullRequest doInBackground(Void... params) {
            try {
                PullRequestActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                PullRequestService service = factory.createPullRequestService();
                return service.getPullRequest(activity.mUserLogin, activity.mRepoName,
                        activity.mPullRequestNumber);
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
        protected void onPostExecute(PullRequest result) {
            PullRequestActivity activity = mTarget.get();
            activity.mLoadingDialog.dismiss();

            if (mException) {
                activity.showError();
            }
            else {
                activity.fillData(result);
            }
        }
    }

}
