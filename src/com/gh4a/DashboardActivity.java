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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.IntentAction;

/**
 * The Dashboard activity.
 */
public class DashboardActivity extends BaseActivity {

    /** The user login. */
    protected String mUserLogin;

    /** The password. */
    protected String mPassword;

    /**
     * Called when the activity is first created.
     * 
     * @param savedInstanceState the saved instance state
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dashboard);

        SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        if (sharedPreferences != null) {
            mUserLogin = sharedPreferences.getString(Constants.User.USER_LOGIN, null);
            mPassword = sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null);
        }
        else {
            Intent intent = new Intent().setClass(getApplicationContext(),
                    Github4AndroidActivity.class);
            startActivity(intent);
        }

        /** Actionbar setup */
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        actionBar.setTitle("Hi, " + mUserLogin);
        actionBar.addAction(new IntentAction(this, new Intent(getApplicationContext(),
                SearchActivity.class), R.drawable.ic_search));

        /** Button news feed */
        Button btnNewsFeed = (Button) findViewById(R.id.btn_news_feed);
        btnNewsFeed.setOnClickListener(new ButtonNewsFeedListener());

        /** Button your actions */
        Button btnYourActions = (Button) findViewById(R.id.btn_your_actions);
        btnYourActions.setOnClickListener(new ButtonYourActionListener());

        /** Button public repositories */
        Button btnPublicRepos = (Button) findViewById(R.id.btn_public_repositories_info);
        btnPublicRepos.setOnClickListener(new ButtonPublicRepositoriesListener());

        /** Button watched repositories */
        Button btnWatchedRepos = (Button) findViewById(R.id.btn_watched_repository_info);
        btnWatchedRepos.setOnClickListener(new ButtonWatchedRepositoriesListener());

        /** Button private repositories */
        Button btnPrivateRepos = (Button) findViewById(R.id.btn_private_repository_info);
        btnPrivateRepos.setOnClickListener(new ButtonPrivateRepositoriesListener());
    }

    /**
     * Callback to be invoked when the button News feed is clicked.
     */
    protected class ButtonNewsFeedListener implements OnClickListener {

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            Intent intent = new Intent()
                    .setClass(DashboardActivity.this, UserPrivateActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            intent.putExtra(Constants.User.USER_AUTH_TOKEN, mPassword);
            intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin);
            intent.putExtra(Constants.SUBTITLE, "News Feed");
            startActivityForResult(intent, 1);
        }
    }

    /**
     * Callback to be invoked when the button Your action is clicked.
     */
    protected class ButtonYourActionListener implements OnClickListener {

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            Intent intent = new Intent().setClass(DashboardActivity.this, UserPublicActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
            intent.putExtra(Constants.ACTIONBAR_TITLE, mUserLogin);
            intent.putExtra(Constants.SUBTITLE, "Your Actions");
            startActivity(intent);
        }
    }

    /**
     * Callback to be invoked when the button Public repositories is clicked.
     */
    protected class ButtonPublicRepositoriesListener implements OnClickListener {

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            Intent intent = new Intent().setClass(DashboardActivity.this,
                    PublicRepoListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            startActivity(intent);
        }
    }

    /**
     * Callback to be invoked when the button Watched repositories is clicked.
     */
    protected class ButtonWatchedRepositoriesListener implements OnClickListener {

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
            Intent intent = new Intent().setClass(DashboardActivity.this,
                    WatchedRepoListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            startActivity(intent);
        }
    }

    /**
     * Callback to be invoked when the button Private repositories is clicked.
     */
    protected class ButtonPrivateRepositoriesListener implements OnClickListener {

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        @Override
        public void onClick(View v) {
        }
    }

}