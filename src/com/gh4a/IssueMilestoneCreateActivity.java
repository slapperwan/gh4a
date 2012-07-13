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

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.utils.StringUtils;

public class IssueMilestoneCreateActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_create_milestone);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_milestone_new);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    
    private static class AddIssueMilestonesTask extends AsyncTask<String, Void, Void> {

        private WeakReference<IssueMilestoneCreateActivity> mTarget;
        private boolean mException;
        
        public AddIssueMilestonesTask(IssueMilestoneCreateActivity activity) {
            mTarget = new WeakReference<IssueMilestoneCreateActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneCreateActivity activity = mTarget.get();
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
                IssueMilestoneCreateActivity activity = mTarget.get();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_create_milestone), false);
                }
                else {
                    activity.openIssueMilestones();
                }
            }
        }
    }
    
    private void openIssueMilestones() {
        Intent intent = new Intent().setClass(this, IssueMilestoneListActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accept_cancel, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        final EditText tvTitle = (EditText) findViewById(R.id.et_title);
        final EditText tvDesc = (EditText) findViewById(R.id.et_desc);
        
        switch (item.getItemId()) {
        case R.id.accept:
            String desc = null;
            
            if (tvDesc.getText() != null) {
                desc = tvDesc.getText().toString();    
            }
            
            if (tvTitle.getText() == null || StringUtils.isBlank(tvTitle.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_milestone_title), false);
            }
            else {
                new AddIssueMilestonesTask(IssueMilestoneCreateActivity.this).execute(tvTitle.getText().toString(), desc);
            }
            return true;

        case R.id.cancel:
            finish();
            return true;
        
        default:
            return true;
        }
    }
}
