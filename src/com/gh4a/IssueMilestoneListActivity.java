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
import java.util.List;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.adapter.MilestoneAdapter;

public class IssueMilestoneListActivity extends BaseActivity implements OnItemClickListener {

    private String mRepoOwner;
    private String mRepoName;
    protected ListView mListView;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.generic_list);
        setUpActionBar();
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_manage_milestones);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        new LoadIssueMilestonesTask(this).execute();
    }
    
    private static class LoadIssueMilestonesTask extends AsyncTask<Void, Integer, List<Milestone>> {

        private WeakReference<IssueMilestoneListActivity> mTarget;
        private boolean mException;

        public LoadIssueMilestonesTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

        @Override
        protected List<Milestone> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    return milestoneService.getMilestones(mTarget.get().mRepoOwner,
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

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<Milestone> result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillData(result);
                }
            }
        }
    }
    
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

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        MilestoneAdapter adapter = (MilestoneAdapter) adapterView.getAdapter();
        Milestone milestone = (Milestone) adapter.getItem(position);
        
//        Intent intent = new Intent().setClass(this, IssueListByMilestoneActivity.class);
//        intent.putExtra(Constants.Repository.REPO_OWNER, mUserLogin);
//        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
//        intent.putExtra(Constants.Issue.ISSUE_MILESTONE_NUMBER, milestone.getNumber());
//        intent.putExtra(Constants.Issue.ISSUE_MILESTONE_TITLE, milestone.getTitle());
//        startActivity(intent);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.create_new, menu);
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.create_new:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueMilestoneCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
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
    
    private static class DeleteIssueMilestoneTask extends AsyncTask<Integer, Void, Void> {

        private WeakReference<IssueMilestoneListActivity> mTarget;
        private boolean mException;
        
        public DeleteIssueMilestoneTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneListActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    
                    Milestone milestone = (Milestone) activity.mListView.getItemAtPosition(params[0]);
                    
                    milestoneService.deleteMilestone(activity.mRepoOwner, activity.mRepoName, 
                            milestone.getNumber());
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    new LoadIssueMilestonesTask(activity).execute();
                }
            }
        }
    }
    
    private static class AddIssueMilestonesTask extends AsyncTask<String, Void, Void> {

        private WeakReference<IssueMilestoneListActivity> mTarget;
        private boolean mException;
        
        public AddIssueMilestonesTask(IssueMilestoneListActivity activity) {
            mTarget = new WeakReference<IssueMilestoneListActivity>(activity);
        }

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
                    
                    milestoneService.createMilestone(activity.mRepoOwner, activity.mRepoName, milestone);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneListActivity activity = mTarget.get();
    
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