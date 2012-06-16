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

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.MilestoneService;
import org.eclipse.egit.github.core.service.RepositoryService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.gh4a.adapter.MilestoneAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;

/**
 * The IssueLabelList activity.
 */
public class IssueMilestoneListActivity extends BaseActivity implements OnItemClickListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    /** The list view. */
    protected ListView mListView;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();
        
        new LoadIssueMilestonesTask(this).execute();
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

        // Issues
        b = new BreadCrumbHolder();
        b.setLabel("Issues");
        b.setTag(Constants.Issue.ISSUES);
        b.setData(data);
        breadCrumbHolders[2] = b;
        
        createBreadcrumb("Milestones", breadCrumbHolders);
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to load issue milestones.
     */
    private static class LoadIssueMilestonesTask extends AsyncTask<Void, Integer, List<Milestone>> {

        /** The target. */
        private WeakReference<IssueMilestoneListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue task.
         *
         * @param activity the activity
         */
        public LoadIssueMilestonesTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<Milestone> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    return milestoneService.getMilestones(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName, null);
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<Milestone> result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
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
     * Fill data into UI components.
     *
     * @param result the result
     */
    private void fillData(List<Milestone> result) {
        mListView = (ListView) findViewById(R.id.list_view);
        mListView.setOnItemClickListener(this);
        MilestoneAdapter adapter = new MilestoneAdapter(this, new ArrayList<Milestone>());
        mListView.setAdapter(adapter);
        registerForContextMenu(mListView);
        
        if (result != null && result.size() > 0) {
            for (Milestone milestone : result) {
                adapter.add(milestone);
            }
        }
        else {
            getApplicationContext().notFoundMessage(this, "Milestones");
        }
        adapter.notifyDataSetChanged();
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MilestoneAdapter adapter = (MilestoneAdapter) adapterView.getAdapter();
        Milestone milestone = (Milestone) adapter.getItem(position);
        
        Intent intent = new Intent().setClass(this, IssueListByMilestoneActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Issue.ISSUE_MILESTONE_NUMBER, milestone.getNumber());
        intent.putExtra(Constants.Issue.ISSUE_MILESTONE_TITLE, milestone.getTitle());
        startActivity(intent);
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.milestones_menu, menu);
        }
        return true;
    }
    
    /* (non-Javadoc)
     * @see com.gh4a.BaseActivity#setMenuOptionItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_milestone:
                if (isAuthorized()) {
                    showCreateMilestoneForm();
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            default:
                return true;
        }
    }
    
    /**
     * Show create milestone form.
     */
    private void showCreateMilestoneForm() {
        final Dialog dialog = new Dialog(this);

        dialog.setContentView(R.layout.issue_create_milestone);
        dialog.setTitle("Create Milestone");
        dialog.setCancelable(true);
        
        final EditText tvTitle = (EditText) dialog.findViewById(R.id.et_title);
        final EditText tvDesc = (EditText) dialog.findViewById(R.id.et_desc);
        
        Button btnCreate = (Button) dialog.findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View arg0) {
                String title = tvTitle.getText().toString();
                String desc = null;
                
                if (tvDesc.getText() != null) {
                    desc = tvDesc.getText().toString();    
                }
                
                if (!StringUtils.isBlank(title)) {
                    dialog.dismiss();
                    new AddIssueMilestonesTask(IssueMilestoneListActivity.this).execute(title, desc);
                }
                else {
                    showMessage(getResources().getString(R.string.issue_error_milestone_title), false);
                }
            }
        });
        
        dialog.show();
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (isAuthorized()) {
            if (v.getId() == R.id.list_view) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                Milestone milestone = (Milestone) mListView.getItemAtPosition(info.position);
                
                menu.setHeaderTitle(milestone.getTitle());
                menu.add(0, Menu.FIRST + 1, 0, "Delete");
                menu.add(0, Menu.FIRST + 2, 0, "View Issues");
            }
        }
    }
    
    /* (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Milestone milestone = (Milestone) mListView.getItemAtPosition(info.position);
        
        switch (item.getItemId()) {
        case Menu.FIRST + 1:
            //try {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Are you sure?")
                       .setCancelable(false)
                       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                               dialog.dismiss();
                               new DeleteIssueMilestoneTask(IssueMilestoneListActivity.this).execute(info.position);
                           }
                       })
                       .setNegativeButton("No", new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                           }
                       });
                AlertDialog alert = builder.create();
                alert.show();
            break;
        case Menu.FIRST + 2:
            Intent intent = new Intent().setClass(this, IssueListByMilestoneActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Issue.ISSUE_MILESTONE_NUMBER, milestone.getNumber());
            intent.putExtra(Constants.Issue.ISSUE_MILESTONE_TITLE, milestone.getTitle());
            startActivity(intent);
            break;
        default:
            break;
        }
        
        return true;
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to delete issue labels.
     */
    private static class DeleteIssueMilestoneTask extends AsyncTask<Integer, Void, Void> {

        /** The target. */
        private WeakReference<IssueMilestoneListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public DeleteIssueMilestoneTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Integer... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    
                    Milestone milestone = (Milestone) activity.mListView.getItemAtPosition(params[0]);
                    
                    milestoneService.deleteMilestone(activity.mUserLogin, activity.mRepoName, 
                            milestone.getNumber());
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    new LoadIssueMilestonesTask(activity).execute();
                }
            }
        }
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to add issue milestone.
     */
    private static class AddIssueMilestonesTask extends AsyncTask<String, Void, Void> {

        /** The target. */
        private WeakReference<IssueMilestoneListActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         */
        public AddIssueMilestonesTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    
                    String title = params[0];
                    String desc = params[1];
                    
                    Milestone milestone = new Milestone();
                    milestone.setTitle(title);
                    milestone.setDescription(desc);
                    
                    milestoneService.createMilestone(activity.mUserLogin, activity.mRepoName, milestone);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true, false);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_create_label), false);
                }
                else {
                    new LoadIssueMilestonesTask(activity).execute();
                }
            }
        }
    }
}