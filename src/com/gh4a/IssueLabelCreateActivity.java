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

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.LabelService;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.utils.StringUtils;

public class IssueLabelCreateActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    private String mSelectectedColor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_create_label);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_label_new);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        buildUi();
    }
    
    private void buildUi() {
        final View viewColor = (View) findViewById(R.id.view_color);
        viewColor.setBackgroundColor(Color.parseColor("#DDDDDD"));//set default color
        
        final View color1 = (View) findViewById(R.id.color_444444);
        color1.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color1.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color2 = (View) findViewById(R.id.color_02d7e1);
        color2.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color2.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color3 = (View) findViewById(R.id.color_02e10c);
        color3.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color3.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color4 = (View) findViewById(R.id.color_0b02e1);
        color4.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color4.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color5 = (View) findViewById(R.id.color_d7e102);
        color5.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color5.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color6 = (View) findViewById(R.id.color_DDDDDD);
        color6.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color6.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color7 = (View) findViewById(R.id.color_e102d8);
        color7.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color7.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
        
        final View color8 = (View) findViewById(R.id.color_e10c02);
        color8.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) { 
                mSelectectedColor = (String) color8.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        });
    }
    
    private static class AddIssueLabelsTask extends AsyncTask<String, Void, Void> {

        private WeakReference<IssueLabelCreateActivity> mTarget;
        private boolean mException;
        
        public AddIssueLabelsTask(IssueLabelCreateActivity activity) {
            mTarget = new WeakReference<IssueLabelCreateActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueLabelCreateActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    
                    Label label = new Label();
                    label.setName(params[0]);
                    label.setColor(activity.mSelectectedColor);
                    
                    labelService.createLabel(activity.mRepoOwner, activity.mRepoName, label);
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
                IssueLabelCreateActivity activity = mTarget.get();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_create_label), false);
                }
                else {
                    activity.openIssueLabels();
                }
            }
        }
    }
    
    private void openIssueLabels() {
        Intent intent = new Intent().setClass(this, IssueLabelListActivity.class);
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
        switch (item.getItemId()) {
        case android.R.id.home:
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
            return true;
        case R.id.accept:
            EditText etLabel = (EditText) findViewById(R.id.et_label);
            if (etLabel.getText() == null || StringUtils.isBlank(etLabel.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_label), false);
            }
            else {
                new AddIssueLabelsTask(IssueLabelCreateActivity.this).execute(etLabel.getText().toString());
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
