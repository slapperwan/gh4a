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
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.gh4a.adapter.UserAdapter;
import com.gh4a.utils.StringUtils;
import com.github.api.v2.schema.User;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.UserService;

/**
 * The UserList activity.
 */
public class UserListActivity extends BaseActivity implements OnItemClickListener {

    /** The search key. */
    protected String mSearchKey;

    /** The title bar. */
    protected String mTitleBar;

    /** The sub title. */
    protected String mSubtitle;

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The user adapter. */
    protected UserAdapter mUserAdapter;

    /** The list view users. */
    protected ListView mListViewUsers;

    /** The row layout. */
    protected int mRowLayout;

    /** The show more data. */
    protected boolean mShowMoreData;
    
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

        mListViewUsers = (ListView) findViewById(R.id.list_view);
        mListViewUsers.setOnItemClickListener(this);

        mUserAdapter = new UserAdapter(this, new ArrayList<User>(), mRowLayout, mShowMoreData);
        mListViewUsers.setAdapter(mUserAdapter);

        new LoadUserListTask(this).execute();
    }

    /**
     * An asynchronous task that runs on a background thread to load user list.
     */
    private static class LoadUserListTask extends AsyncTask<Void, Integer, List<User>> {

        /** The target. */
        private WeakReference<UserListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load user list task.
         *
         * @param activity the activity
         */
        public LoadUserListTask(UserListActivity activity) {
            mTarget = new WeakReference<UserListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<User> doInBackground(Void... params) {
            try {
                return mTarget.get().getUsers();
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
        protected void onPostExecute(List<User> result) {
            mTarget.get().mLoadingDialog.dismiss();
            if (mException) {
                mTarget.get().showError();
            }
            else {
                mTarget.get().fillData(result);
            }
        }
    }

    /**
     * Fill data into UI components.
     * 
     * @param users the users
     */
    protected void fillData(List<User> users) {
        if (users != null && users.size() > 0) {
            mUserAdapter.notifyDataSetChanged();
            for (User user : users) {
                mUserAdapter.add(user);
            }
        }
        mUserAdapter.notifyDataSetChanged();
    }

    /**
     * Gets the users.
     *
     * @return the users
     * @throws GitHubException the git hub exception
     */
    protected List<User> getUsers() throws GitHubException {
        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
        UserService service = factory.createUserService();
        return service.searchUsersByName(mSearchKey);
    }

    /**
     * Sets the request data.
     */
    protected void setRequestData() {
        mSearchKey = getIntent().getExtras().getString("searchKey");
        mShowMoreData = true;
    }

    /**
     * Sets the title bar.
     */
    protected void setTitleBar() {
        mTitleBar = "User";// default
    }

    /**
     * Sets the subtitle.
     */
    protected void setSubtitle() {
        mSubtitle = "List";// default
    }

    /**
     * Sets the row layout.
     */
    protected void setRowLayout() {
        mRowLayout = R.layout.row_gravatar_1;
    }

    /**
     * Sets the bread crumbs.
     */
    protected void setBreadCrumbs() {
        // no breadcrumbs
    }

    /*
     * (non-Javadoc)
     * @see
     * android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget
     * .AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        User user = (User) adapterView.getAdapter().getItem(position);
        Intent intent = new Intent().setClass(UserListActivity.this, UserActivity.class);
        intent.putExtra(Constants.User.USER_LOGIN, (String) user.getLogin());
        intent.putExtra(Constants.User.USER_NAME, (String) user.getName());
        startActivity(intent);
    }
}