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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.gh4a.utils.ImageDownloader;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.User;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.UserService;

/**
 * The UserInfo activity.
 */
public class UserActivity extends BaseActivity implements OnClickListener {

    /** The user login. */
    protected String mUserLogin;

    /** The user name. */
    protected String mUserName;

    /** The watched repos count. */
    protected int mWatchedReposCount;

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

        setContentView(R.layout.user);
        setUpActionBar();

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserName = data.getString(Constants.User.USER_NAME);

        new LoadUserInfoTask(this).execute();
    }

    /**
     * An asynchronous task that runs on a background thread to load user info.
     */
    private static class LoadUserInfoTask extends AsyncTask<Void, Integer, User> {

        /** The target. */
        private WeakReference<UserActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load user info task.
         *
         * @param activity the activity
         */
        public LoadUserInfoTask(UserActivity activity) {
            mTarget = new WeakReference<UserActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected User doInBackground(Void... arg0) {
            try {
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                UserService userService = factory.createUserService();
                return userService.getUserByUsername(mTarget.get().mUserLogin);
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
        protected void onPostExecute(User result) {
            mTarget.get().mLoadingDialog.dismiss();
            if (mException) {
                mTarget.get().showError();
            }
            else {
                mTarget.get().fillData(result);
                new LoadWatchedReposTask(mTarget.get()).execute();
            }
        }
    }

    /**
     * An asynchronous task that runs on a background thread to load watched
     * repos.
     */
    private static class LoadWatchedReposTask extends AsyncTask<Void, Integer, Integer> {

        /** The target. */
        private WeakReference<UserActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load watched repos task.
         *
         * @param activity the activity
         */
        public LoadWatchedReposTask(UserActivity activity) {
            mTarget = new WeakReference<UserActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Integer doInBackground(Void... arg0) {
            try {
                GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
                UserService userService = factory.createUserService();
                mTarget.get().mWatchedReposCount = userService.getWatchedRepositories(
                        mTarget.get().mUserLogin).size();
                return mTarget.get().mWatchedReposCount;
            }
            catch (GitHubException e) {
                Log.e(Constants.LOG_TAG, e.getMessage(), e);
                mException = true;
                return null;
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Integer result) {
            if (mException) {
                mTarget.get().showError();
            }
            else {
                Button btnWatchedRepos = (Button) mTarget.get()
                        .findViewById(R.id.btn_watched_repos);
                btnWatchedRepos.setText(String.valueOf(result));
            }
        }
    }

    /**
     * Fill data into UI components.
     *
     * @param user the user
     */
    protected void fillData(User user) {
        ImageView ivGravatar = (ImageView) findViewById(R.id.iv_gravatar);
        ImageDownloader.getInstance().download(user.getGravatarId(), ivGravatar, 80);

        TextView tvName = (TextView) findViewById(R.id.tv_name);
        TextView tvCreated = (TextView) findViewById(R.id.tv_created_at);
        LinearLayout llEmail = (LinearLayout) findViewById(R.id.ll_user_email);
        LinearLayout llWebsite = (LinearLayout) findViewById(R.id.ll_user_website);
        LinearLayout llLocation = (LinearLayout) findViewById(R.id.ll_user_location);

        Button btnNews = (Button) findViewById(R.id.btn_news);
        btnNews.setOnClickListener(this);
        
        Button btnYourActions = (Button) findViewById(R.id.btn_your_actions);
        if (mUserLogin.equals(getAuthUsername())) {
            btnYourActions.setOnClickListener(this);
            btnYourActions.setVisibility(View.VISIBLE);
        }
        else {
            btnYourActions.setVisibility(View.GONE);
        }

        Button btnPublicRepos = (Button) findViewById(R.id.btn_pub_repos);
        btnPublicRepos.setOnClickListener(this);

        Button btnWatchedRepos = (Button) findViewById(R.id.btn_watched_repos);
        btnWatchedRepos.setOnClickListener(this);

        Button btnFollowers = (Button) findViewById(R.id.btn_followers);
        btnFollowers.setOnClickListener(this);

        Button btnFollowing = (Button) findViewById(R.id.btn_following);
        btnFollowing.setOnClickListener(this);

        tvName.setText(StringUtils.formatName(user.getLogin(), user.getName()));
        if (Constants.User.USER_TYPE_ORG.equals(user.getType())) {
            tvName.append(" (");
            tvName.append(Constants.User.USER_TYPE_ORG);
            tvName.append(")");
        }
        tvCreated.setText(user.getCreatedAt() != null ? "Member Since "
                + StringUtils.formatDate(user.getCreatedAt()) : "");

        //show email row if not blank
        if (!StringUtils.isBlank(user.getEmail())) {
            TextView tvEmail = (TextView) findViewById(R.id.tv_email);
            tvEmail.setText(user.getEmail());
            llEmail.setVisibility(View.VISIBLE);
        }
        else {
            llEmail.setVisibility(View.GONE);
        }
        
        //show website if not blank
        if (!StringUtils.isBlank(user.getBlog())) {
            TextView tvWebsite = (TextView) findViewById(R.id.tv_website);
            tvWebsite.setText(user.getBlog());
            llWebsite.setVisibility(View.VISIBLE);
        }
        else {
            llWebsite.setVisibility(View.GONE);
        }
        
        //Show location if not blank
        if (!StringUtils.isBlank(user.getLocation())) {
            TextView tvLocation = (TextView) findViewById(R.id.tv_location);
            tvLocation.setText(user.getLocation());
            llLocation.setVisibility(View.VISIBLE);
        }
        else {
            llLocation.setVisibility(View.GONE);
        }
        
        btnPublicRepos.setText(String.valueOf(user.getPublicRepoCount()));
        btnWatchedRepos.setText(String.valueOf(mWatchedReposCount));
        btnFollowers.setText(String.valueOf(user.getFollowersCount()));
        btnFollowing.setText(String.valueOf(user.getFollowingCount()));
    }

    /*
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    public void onClick(View view) {
        int id = view.getId();

        if (id == R.id.btn_news) {
            getFeeds(view);
        }
        else if (id == R.id.btn_your_actions) {
            getYourActions(view);
        }
        else if (id == R.id.btn_pub_repos) {
            getPublicRepos(view);
        }
        else if (id == R.id.btn_followers) {
            getFollowers(view);
        }
        else if (id == R.id.btn_following) {
            getFollowing(view);
        }
        else if (id == R.id.btn_watched_repos) {
            getWatchedRepos(view);
        }
    }

    /**
     * Gets the feeds when Activity button clicked.
     *
     * @param view the view
     * @return the feeds
     */
    public void getFeeds(View view) {
        Intent intent = new Intent().setClass(this, UserPrivateActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin
                + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : ""));
        intent.putExtra(Constants.SUBTITLE, "Public Activity");
        startActivity(intent);
    }

    public void getYourActions(View view) {
        Intent intent = new Intent().setClass(this, UserPublicActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin);
        intent.putExtra(Constants.SUBTITLE, "Your Actions");
        startActivity(intent);
    }
    /**
     * Gets the public repos when Public Repository button clicked.
     *
     * @param view the view
     * @return the public repos
     */
    public void getPublicRepos(View view) {
        Intent intent = new Intent().setClass(this, PublicRepoListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        startActivity(intent);
    }

    /**
     * Gets the followers when Followers button clicked.
     *
     * @param view the view
     * @return the followers
     */
    public void getFollowers(View view) {
        Intent intent = new Intent().setClass(this, FollowerFollowingListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin
                + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : ""));
        intent.putExtra(Constants.SUBTITLE, "Followers");
        intent.putExtra(Constants.FIND_FOLLOWER, true);
        startActivity(intent);
    }

    /**
     * Gets the following when Following button clicked.
     *
     * @param view the view
     * @return the following
     */
    public void getFollowing(View view) {
        Intent intent = new Intent().setClass(this, FollowerFollowingListActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
        intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin
                + (!StringUtils.isBlank(mUserName) ? " - " + mUserName : ""));
        intent.putExtra(Constants.SUBTITLE, "Following");
        intent.putExtra(Constants.FIND_FOLLOWER, false);
        startActivity(intent);
    }

    /**
     * Gets the watched repos.
     * 
     * @param view the view
     * @return the watched repos
     */
    public void getWatchedRepos(View view) {
        Intent intent = new Intent().setClass(this, WatchedRepoListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.User.USER_NAME, mUserName);
        startActivity(intent);
    }
}