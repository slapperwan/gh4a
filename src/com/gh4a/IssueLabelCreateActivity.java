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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
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
    private ProgressDialog mProgressDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_create_label);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_label_new);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        buildUi();
    }
    
    private void buildUi() {
        final View viewColor = findViewById(R.id.view_color);
        final ViewGroup colors = (ViewGroup) findViewById(R.id.colors);
        int count = colors.getChildCount();

        final OnClickListener listener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mSelectectedColor = (String) v.getTag();
                viewColor.setBackgroundColor(Color.parseColor("#" + mSelectectedColor));
            }
        };
        
        for (int i = 0; i < count; i++) {
            colors.getChildAt(i).setOnClickListener(listener);
        }

        // set default color
        listener.onClick(colors.getChildAt(0));
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
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.navigation_cancel_dark);
            menu.getItem(1).setIcon(R.drawable.navigation_accept_dark);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.accept) {
            EditText etLabel = (EditText) findViewById(R.id.et_label);
            String name = etLabel.getText() == null ? null : etLabel.getText().toString();

            if (StringUtils.isBlank(name)) {
                etLabel.setError(getString(R.string.issue_error_label));
            }
            else {
                new AddIssueLabelsTask(IssueLabelCreateActivity.this).execute(name);
            }
        } else if (itemId == R.id.cancel) {
            finish();
        }
        return true;
    }

    private static class AddIssueLabelsTask extends AsyncTask<String, Void, Void> {
        private WeakReference<IssueLabelCreateActivity> mTarget;
        private boolean mException;

        public AddIssueLabelsTask(IssueLabelCreateActivity activity) {
            mTarget = new WeakReference<IssueLabelCreateActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            IssueLabelCreateActivity activity = mTarget.get();
            if (activity == null) {
                return null;
            }

            try {
                GitHubClient client = new GitHubClient();
                client.setOAuth2Token(activity.getAuthToken());
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
            return null;
        }

        @Override
        protected void onPreExecute() {
            IssueLabelCreateActivity activity = mTarget.get();
            if (activity != null) {
                activity.mProgressDialog = activity.showProgressDialog(
                        activity.getString(R.string.saving_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            IssueLabelCreateActivity activity = mTarget.get();
            if (activity != null) {
                activity.stopProgressDialog(activity.mProgressDialog);
                if (mException) {
                    activity.showMessage(activity.getString(R.string.issue_error_create_label), false);
                }
                else {
                    activity.openIssueLabels();
                }
            }
        }
    }
}
