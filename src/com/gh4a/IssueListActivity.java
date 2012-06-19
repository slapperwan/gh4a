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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.adapter.IssueAdapter;
import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.db.DbHelper;
import com.gh4a.holder.BreadCrumbHolder;
import com.markupartist.android.widget.ActionBar;
import com.markupartist.android.widget.ActionBar.Action;
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
    
    protected String mSortBy;
    
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
        setRowLayout();

        mListViewIssues = (ListView) findViewById(R.id.list_view);
        mListViewIssues.setOnItemClickListener(this);

        mIssueAdapter = new IssueAdapter(this, new ArrayList<Issue>(), mRowLayout);
        mListViewIssues.setAdapter(mIssueAdapter);

        mSortBy = "created";
        new LoadIssueListTask(this, true).execute();
    }

    /*
     * (non-Javadoc)
     * @see com.gh4a.BaseActivity#setUpActionBar()
     */
    @Override
    public void setUpActionBar() {
        ActionBar actionBar = (ActionBar) findViewById(R.id.actionbar);
        if (isAuthorized()) {
            Intent intent = new Intent().setClass(getApplicationContext(), UserActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, getAuthLogin());
            actionBar.setHomeAction(new IntentAction(this, intent, R.drawable.ic_home));
        }
        actionBar.addAction(new SortSubmittedAction());
    }

    private class SortSubmittedAction implements Action {

        @Override
        public int getDrawable() {
            return android.R.drawable.ic_menu_sort_by_size;
        }

        @Override
        public void performAction(View view) {
            AlertDialog.Builder builder = new AlertDialog.Builder(
                    new ContextThemeWrapper(IssueListActivity.this, android.R.style.Theme));
            builder.setCancelable(true);
            builder.setTitle(R.string.sort_by);
            
            final String[] sortBy = getResources().getStringArray(R.array.sort_by_item);
            builder.setItems(sortBy, 
                    new DialogInterface.OnClickListener() {
                
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    mSortBy = sortBy[item];
                    mIssueAdapter.clear();
                    new LoadIssueListTask(IssueListActivity.this, true).execute();
                }
            });
            
            builder.create();
            builder.show();
        }

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

        createBreadcrumb(mState + " Issues", breadCrumbHolders);
    }

    public String getSubTitleAfterLoaded(int numberOfIssues) {
        if (numberOfIssues != -1) {
            return mState + " Issues (" + numberOfIssues + ")";
        }
        else {
            return mState + " Issues";
        }
    }
    
    public void setRowLayout() {
        mRowLayout = R.layout.row_issue;
    }
    
    public List<Issue> getIssues() throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        IssueService issueService = new IssueService(client);
        Map<String, String> filterData = new HashMap<String, String>();
        filterData.put("state", mState);
        filterData.put("sort", mSortBy.toLowerCase());
        return issueService.getIssues(mUserLogin, mRepoName, filterData);
    }
    
    /**
     * An asynchronous task that runs on a background thread to load issue list.
     */
    private static class LoadIssueListTask extends AsyncTask<Void, Integer, List<Issue>> {

        /** The target. */
        private WeakReference<IssueListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        private boolean mHideMainView;

        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public LoadIssueListTask(IssueListActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueListActivity>(activity);
            mHideMainView = hideMainView;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Issue> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getIssues();
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, mHideMainView);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Issue> result) {
            if (mTarget.get() != null) {
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
            for (Issue issue : issues) {
                mIssueAdapter.add(issue);
            }
            ((TextView) findViewById(R.id.tv_subtitle)).setText(getSubTitleAfterLoaded(issues.size()));
            mIssueAdapter.notifyDataSetChanged();
        }
        else {
            ((TextView) findViewById(R.id.tv_subtitle)).setText(getSubTitleAfterLoaded(-1));
            getApplicationContext().notFoundMessage(this, R.plurals.issue);
        }
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
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        inflater.inflate(R.menu.bookmark_menu, menu);
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_open_issues:
                getApplicationContext().openIssueListActivity(this, mUserLogin, mRepoName, Constants.Issue.ISSUE_STATE_OPEN);
                return true;
            case R.id.view_closed_issues:
                getApplicationContext().openIssueListActivity(this, mUserLogin, mRepoName, Constants.Issue.ISSUE_STATE_CLOSED);
                return true;
            case R.id.create_issue:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(IssueListActivity.this, IssueCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            case R.id.view_labels:
                Intent intent = new Intent().setClass(IssueListActivity.this, IssueLabelListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.view_milestones:
                intent = new Intent().setClass(IssueListActivity.this, IssueMilestoneListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;    
                
            default:
                return true;
        }
    }
    
    @Override
    public void openBookmarkActivity() {
        Intent intent = new Intent().setClass(this, BookmarkListActivity.class);
        intent.putExtra(Constants.Bookmark.NAME, "Issues at " + mUserLogin + "/" + mRepoName);
        intent.putExtra(Constants.Bookmark.OBJECT_TYPE, Constants.Bookmark.OBJECT_TYPE_ISSUE);
        startActivityForResult(intent, 100);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
           if (resultCode == Constants.Bookmark.ADD) {
               DbHelper db = new DbHelper(this);
               Bookmark b = new Bookmark();
               b.setName("Issues at " + mUserLogin + "/" + mRepoName);
               b.setObjectType(Constants.Bookmark.OBJECT_TYPE_ISSUE);
               b.setObjectClass(IssueListActivity.class.getName());
               long id = db.saveBookmark(b);
               
               BookmarkParam[] params = new BookmarkParam[3];
               BookmarkParam param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_OWNER);
               param.setValue(mUserLogin);
               params[0] = param;
               
               param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Repository.REPO_NAME);
               param.setValue(mRepoName);
               params[1] = param;
               
               param = new BookmarkParam();
               param.setBookmarkId(id);
               param.setKey(Constants.Issue.ISSUE_STATE);
               param.setValue(mState);
               params[2] = param;
               
               db.saveBookmarkParam(params);
           }
        }
     }
}