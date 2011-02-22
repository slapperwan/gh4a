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
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.FollowerFollowingAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.UserService;

/**
 * The FollowerFollowingList activity.
 */
public class FollowerFollowingListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The sub title. */
    protected String mSubtitle;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The follower following adapter. */
    protected FollowerFollowingAdapter mFollowerFollowingAdapter;

    /** The list view users. */
    protected ListView mListViewUsers;

    /** The find followers. */
    protected boolean mFindFollowers;// flag to search followers or following

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

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mSubtitle = data.getString(Constants.SUBTITLE);
        mFindFollowers = data.getBoolean("FIND_FOLLOWER");

        setBreadCrumb();

        mListViewUsers = (ListView) findViewById(R.id.list_view);
        mListViewUsers.setOnItemClickListener(this);

        mFollowerFollowingAdapter = new FollowerFollowingAdapter(this, new ArrayList<String>());
        mListViewUsers.setAdapter(mFollowerFollowingAdapter);

        new LoadListTask(this).execute();
    }

    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[1];

        // common data
        HashMap<String, String> data = new HashMap<String, String>();
        data.put(Constants.User.USER_LOGIN, mUserLogin);

        // User
        BreadCrumbHolder b = new BreadCrumbHolder();
        b.setLabel(mUserLogin);
        b.setTag(Constants.User.USER_LOGIN);
        b.setData(data);
        breadCrumbHolders[0] = b;

        createBreadcrumb(mSubtitle, breadCrumbHolders);
    }

    /**
     * An asynchronous task that runs on a background thread to load list.
     */
    private static class LoadListTask extends AsyncTask<Void, Integer, List<String>> {

        /** The target. */
        private WeakReference<FollowerFollowingListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load list task.
         *
         * @param activity the activity
         */
        public LoadListTask(FollowerFollowingListActivity activity) {
            mTarget = new WeakReference<FollowerFollowingListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<String> doInBackground(Void... params) {
            try {
                FollowerFollowingListActivity activity = mTarget.get();
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                UserService service = factory.createUserService();
                List<String> usernames = new ArrayList<String>();
                if (activity.mFindFollowers) {
                    usernames = service.getUserFollowers(activity.mUserLogin);
                }
                else {
                    usernames = service.getUserFollowing(activity.mUserLogin);
                }
                return usernames;
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
            FollowerFollowingListActivity activity = mTarget.get();
            activity.mLoadingDialog = LoadingDialog.show(activity, true, true);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<String> result) {
            FollowerFollowingListActivity activity = mTarget.get();
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
     * Fill data into UI components.
     * 
     * @param usernames the usernames
     */
    protected void fillData(List<String> usernames) {
        if (usernames != null && usernames.size() > 0) {
            mFollowerFollowingAdapter.notifyDataSetChanged();
            for (String username : usernames) {
                mFollowerFollowingAdapter.add(username);
            }
        }
        mFollowerFollowingAdapter.notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String username = (String) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(FollowerFollowingListActivity.this,
                UserActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, username);
        startActivity(intent);
    }
}
