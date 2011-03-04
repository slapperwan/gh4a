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
<<<<<<< HEAD
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
=======
import android.widget.ListView;
>>>>>>> upstream/master

import com.gh4a.adapter.YourActionsAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.holder.YourActionFeed;
import com.gh4a.utils.RssParser;
<<<<<<< HEAD
import com.github.api.v2.services.GitHubException;
=======
import com.github.api.v2.schema.Feed;
import com.github.api.v2.schema.FeedEntry;
import com.github.api.v2.services.FeedService;
import com.github.api.v2.services.GitHubException;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.auth.Authentication;
import com.github.api.v2.services.auth.LoginPasswordAuthentication;
>>>>>>> upstream/master

/**
 * The UserYourActions activity.
 */
<<<<<<< HEAD
public class UserYourActionsActivity extends BaseActivity implements OnItemClickListener {
=======
public class UserYourActionsActivity extends BaseActivity {
>>>>>>> upstream/master

    /** The user login. */
    protected String mUserLogin;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        
        setBreadCrumb();
        
        new LoadActivityListTask(this).execute();
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

        createBreadcrumb(getResources().getString(R.string.user_your_actions), breadCrumbHolders);
    }
    
    private static class LoadActivityListTask extends AsyncTask<Void, Integer, List<YourActionFeed>> {

        /** The target. */
        private WeakReference<UserYourActionsActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load activity list task.
         *
         * @param activity the activity
         */
        public LoadActivityListTask(UserYourActionsActivity activity) {
            mTarget = new WeakReference<UserYourActionsActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<YourActionFeed> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getFeeds();
                }
                catch (GitHubException e) {
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<YourActionFeed> result) {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog.dismiss();
                if (mException) {
                    mTarget.get().showError();
                }
                else {
                    mTarget.get().fillData(result);
                }
            }
        }
    }
    
    public List<YourActionFeed> getFeeds() throws GitHubException {
<<<<<<< HEAD
        RssParser p = new RssParser("https://github.com/slapperwan.private.actor.atom");
=======
        RssParser p = new RssParser("http://github.com/slapperwan.private.atom");
>>>>>>> upstream/master
        return p.parse();
        
//        GitHubServiceFactory factory = GitHubServiceFactory.newInstance();
//        FeedService feedService = factory.createFeedService();
//        Authentication auth = new LoginPasswordAuthentication(getAuthUsername(), getAuthPassword());
//        feedService.setAuthentication(auth);
//        Feed feed = feedService.getPrivateUserFeed(getAuthUsername(), 100);
//        return feed.getEntries();
    }
    
    protected void fillData(List<YourActionFeed> feedEntries) {
        YourActionsAdapter adapter = new YourActionsAdapter(this, new ArrayList<YourActionFeed>());
        ListView listView = (ListView) findViewById(R.id.list_view);
<<<<<<< HEAD
        listView.setOnItemClickListener(this);
=======
>>>>>>> upstream/master
        listView.setAdapter(adapter);
        
        if (feedEntries != null && feedEntries.size() > 0) {
            for (YourActionFeed entry : feedEntries) {
                adapter.add(entry);
            }
            adapter.notifyDataSetChanged();
        }
    }
<<<<<<< HEAD

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        YourActionFeed feed = (YourActionFeed) adapterView.getAdapter().getItem(position);
        getApplicationContext().openRepositoryInfoActivity(this, feed.getRepoOWner(), feed.getRepoName());
    }
=======
>>>>>>> upstream/master
}