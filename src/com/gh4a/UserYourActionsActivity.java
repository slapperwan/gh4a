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

import com.gh4a.adapter.YourActionsAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.holder.YourActionFeed;
import com.gh4a.utils.RssParser;
import com.github.api.v2.schema.UserFeed;

/**
 * The UserYourActions activity.
 */
public class UserYourActionsActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    protected String mUserLogin;
    
    protected String mUrl;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.User.USER_LOGIN);
        mUrl = getIntent().getExtras().getString(Constants.Repository.REPO_URL);
        
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

        createBreadcrumb(getResources().getString(R.string.user_private_activity), breadCrumbHolders);
    }
    
    private static class LoadActivityListTask extends AsyncTask<Void, Integer, List<YourActionFeed>> {

        /** The target. */
        private WeakReference<UserYourActionsActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        private boolean isAuthError; 

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
                RssParser p = new RssParser(mTarget.get().mUrl, mTarget.get().getAuthUsername(), mTarget.get().getAuthPassword());
                try {
                    return p.parse();
                }
                catch (Exception e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    if ("Received authentication challenge is null".equalsIgnoreCase(e.getMessage())) {
                        mException = true;
                        isAuthError = true;
                    }
                    else {
                        mException = true;
                        isAuthError = false;
                    }
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
                    if (isAuthError) {
                        mTarget.get().showMessage("Invalid API Token.", false);
                    }
                    else {
                        mTarget.get().showError();
                    }
                }
                else {
                    if (result != null) {
                        mTarget.get().fillData(result);
                    }
                }
            }
        }
    }
    
    protected void fillData(List<YourActionFeed> feedEntries) {
        YourActionsAdapter adapter = new YourActionsAdapter(this, new ArrayList<YourActionFeed>());
        ListView listView = (ListView) findViewById(R.id.list_view);
        listView.setOnItemClickListener(this);
        listView.setAdapter(adapter);
        
        if (feedEntries != null && feedEntries.size() > 0) {
            for (YourActionFeed entry : feedEntries) {
                adapter.add(entry);
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        YourActionFeed feed = (YourActionFeed) adapterView.getAdapter().getItem(position);
        String event = feed.getEvent();
        String actionPath = feed.getActionPath();
        if (UserFeed.Type.PUSH_EVENT.value().equals(event)
                && actionPath.contains("compare")) {
            Intent intent = new Intent().setClass(this, CompareActivity.class);
            String[] commitMsgs = feed.getContent().split("\n");
            for (String msg : commitMsgs) {
                String stripAllmsg = msg.replaceAll("\n", "").replaceAll("\r\n", "");
                String[] msgPart = msg.split(" ");
                if (msgPart[0].matches("[0-9a-zA-Z]{7}") && msgPart.length > 1) {
                    if (!msg.substring(msg.indexOf(msgPart[1]), msg.length()).contains("more commits")) {
                        String[] shas = new String[4];
                        shas[0] = msgPart[0];
                        shas[1] = feed.getEmail();
                        shas[2] = msg.substring(msg.indexOf(msgPart[1]), msg.length());
                        shas[3] = feed.getAuthor();//TODO, get actual commit author from content
                        intent.putExtra("sha" + shas[0], shas);
                    }
                }
            }
            
            intent.putExtra(Constants.Repository.REPO_OWNER, feed.getRepoOWner());
            intent.putExtra(Constants.Repository.REPO_NAME, feed.getRepoName());
            intent.putExtra(Constants.Repository.REPO_URL, feed.getLink());
            startActivity(intent);
        }
        else if (UserFeed.Type.FOLLOW_EVENT.value().equals(event)) {
            String[] title = feed.getTitle().split(" ");
            String username = title[3];
            getApplicationContext().openUserInfoActivity(this, username, null);
        }
        else if (UserFeed.Type.ISSUES_EVENT.value().equals(event)) {
            String[] title = feed.getTitle().split(" ");
            String issueNumber = title[3];
            getApplicationContext().openIssueActivity(this, feed.getRepoOWner(), feed.getRepoName(), Integer.parseInt(issueNumber));
        }
        else if (UserFeed.Type.GOLLUM_EVENT.value().equals(event)) {
            getApplicationContext().openBrowser(this, feed.getLink());
        }
        else if (UserFeed.Type.PULL_REQUEST_EVENT.value().equals(event)) {
            String[] title = feed.getTitle().split(" ");
            String issueNumber = title[4];
            getApplicationContext().openPullRequestActivity(this, feed.getRepoOWner(), feed.getRepoName(), Integer.parseInt(issueNumber));
        }
        else if (UserFeed.Type.ISSUE_COMMENT_EVENT.value().equals(event)) {
            String[] title = feed.getTitle().split(" ");
            String issueNumber = title[4];
            if ("request".equals(issueNumber)) {//comment in pull request
                issueNumber = title[5];
            }
            getApplicationContext().openIssueActivity(this, feed.getRepoOWner(), feed.getRepoName(), Integer.parseInt(issueNumber));
        }
        else {
            if (feed.getRepoOWner() != null
                    && feed.getRepoName() != null) {
                
                //url is https://github.com/organizations/:organization/:....
                if ("organizations".equals(feed.getRepoOWner())) {
                    getApplicationContext().openUserInfoActivity(this, feed.getRepoName(), null);
                }
                else {
                    getApplicationContext().openRepositoryInfoActivity(this, feed.getRepoOWner(), feed.getRepoName());
                }
            }
            else {
                getApplicationContext().notFoundMessage(this, R.plurals.repository);
            }
        }
    }
}
