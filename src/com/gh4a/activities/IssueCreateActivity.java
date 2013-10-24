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
package com.gh4a.activities;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.StringUtils;

public class IssueCreateActivity extends BaseSherlockFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    private ActionBar mActionBar;
    private List<Label> mSelectedLabels;
    private Milestone mSelectedMilestone;
    private User mSelectedAssignee;
    private EditText mEtTitle;
    private EditText mEtDesc;
    private TextView mTvSelectedMilestone;
    private TextView mTvSelectedAssignee;
    private List<Label> mAllLabel;
    private List<Milestone> mAllMilestone;
    private List<User> mAllAssignee;
    private LinearLayout mLinearLayoutLabels;
    private int mIssueNumber;
    private boolean mEditMode; 
    private Issue mEditIssue;
    private ProgressDialog mProgressDialog;

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueCreateActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            if (!checkForError(result)) {
                mAllLabel = result.getData();
                fillLabels();
            }
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback = new LoaderCallbacks<List<Milestone>>() {
        @Override
        public Loader<LoaderResult<List<Milestone>>> onCreateLoader(int id, Bundle args) {
            return new MilestoneListLoader(IssueCreateActivity.this, mRepoOwner, mRepoName, "open");
        }
        @Override
        public void onResultReady(LoaderResult<List<Milestone>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mAllMilestone = result.getData();
                showMilestonesDialog();
            }
        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback = new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new CollaboratorListLoader(IssueCreateActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mAllAssignee = result.getData();
                showAssigneesDialog();
            }
        }
    };

    private LoaderCallbacks<Boolean> mIsCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(IssueCreateActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            LinearLayout collaboratorLayout = (LinearLayout) findViewById(R.id.for_collaborator);
            if (result.getData()) {
                collaboratorLayout.setVisibility(View.VISIBLE);
                getSupportLoaderManager().getLoader(0).forceLoad();
            }
            else {
                collaboratorLayout.setVisibility(View.GONE);
            }
        }
    };

    private LoaderCallbacks<Issue> mIssueCallback = new LoaderCallbacks<Issue>() {
        @Override
        public Loader<LoaderResult<Issue>> onCreateLoader(int id, Bundle args) {
            return new IssueLoader(IssueCreateActivity.this, mRepoOwner, mRepoName, mIssueNumber);
        }
        @Override
        public void onResultReady(LoaderResult<Issue> result) {
            hideLoading();
            mEditIssue = result.getData();
            getSupportLoaderManager().getLoader(4).forceLoad();
            fillIssueData();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mSelectedLabels = new ArrayList<Label>();
        Bundle data = getIntent().getExtras();
        
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mIssueNumber = data.getInt(Constants.Issue.ISSUE_NUMBER);
        if (mIssueNumber != 0) {
            mEditMode = true;
        }
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_create);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mEditMode ? getString(R.string.issue_edit_title, mIssueNumber)
                : getString(R.string.issue_create));
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        mEtTitle = (EditText) findViewById(R.id.et_title);
        mEtDesc = (EditText) findViewById(R.id.et_desc);
        
        mLinearLayoutLabels = (LinearLayout) findViewById(R.id.ll_labels);
        mTvSelectedMilestone = (EditText) findViewById(R.id.et_milestone);
        
        mTvSelectedAssignee = (EditText) findViewById(R.id.et_assignee);
        
        TextView tvIssueLabelAdd = (TextView) findViewById(R.id.tv_issue_label_add);
        tvIssueLabelAdd.setTypeface(Gh4Application.get(this).boldCondensed);
        tvIssueLabelAdd.setTextColor(getResources().getColor(R.color.highlight));
        
        LinearLayout collaboratorLayout = (LinearLayout) findViewById(R.id.for_collaborator);
        collaboratorLayout.setVisibility(View.GONE);

        getSupportLoaderManager().initLoader(0, null, mLabelCallback);
        getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
        getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
        getSupportLoaderManager().initLoader(4, null, mIsCollaboratorCallback);
        
        if (mEditMode) {
            showLoading();
            getSupportLoaderManager().initLoader(3, null, mIssueCallback);
            getSupportLoaderManager().getLoader(3).forceLoad();
        }
        else {
            hideLoading();
            getSupportLoaderManager().getLoader(4).forceLoad();
        }
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
    protected void navigateUp() {
        Gh4Application.get(this).openIssueListActivity(this, mRepoOwner, mRepoName,
                Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.accept:
            if (mEtTitle.getText() == null || StringUtils.isBlank(mEtTitle.getText().toString())) {
                mEtTitle.setError(getString(R.string.issue_error_title));
            }
            else {
                new SaveIssueTask(this, false).execute();
            }
            return true;
        case R.id.cancel:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    public void showMilestonesDialog(View v) {
        if (mAllMilestone == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().getLoader(1).forceLoad();
        }
        else {
            showMilestonesDialog();
        }
    }
    
    public void showAssigneesDialog(View v) {
        if (mAllAssignee == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().getLoader(2).forceLoad();
        }
        else {
            showAssigneesDialog();
        }
    }
    
    private static class SaveIssueTask extends AsyncTask<String, Void, Boolean> {

        private WeakReference<IssueCreateActivity> mTarget;
        private boolean mException;

        public SaveIssueTask(IssueCreateActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueCreateActivity>(activity);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueCreateActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    IssueService issueService = new IssueService(client);
                    
                    Issue issue = new Issue();
                    if (activity.mEditMode) {
                        issue = activity.mEditIssue;
                    }
                    
                    issue.setTitle(activity.mEtTitle.getText().toString());
                    issue.setBody(activity.mEtDesc.getText().toString());
                    
                    issue.setLabels(activity.mSelectedLabels);
                    issue.setMilestone(activity.mSelectedMilestone);
                    issue.setAssignee(activity.mSelectedAssignee);
                    
                    if (activity.mEditMode) {
                        activity.mEditIssue = issueService.editIssue(activity.mRepoOwner, 
                                activity.mRepoName, issue);
                    }
                    else {
                        activity.mEditIssue = issueService.createIssue(activity.mRepoOwner, 
                                activity.mRepoName, issue);
                    }
                    
                    return true;
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return false;
            }
        }

        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                IssueCreateActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.saving_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueCreateActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
    
                if (mException) {
                    activity.showError(false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(
                            activity.mEditMode ? R.string.issue_success_edit : R.string.issue_success_create),
                            false);
                    Gh4Application.get(activity).openIssueActivity(activity, 
                            activity.mRepoOwner, 
                            activity.mRepoName,
                            activity.mEditIssue.getNumber(),
                            Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
            }
        }
    }
    
    private void showMilestonesDialog() {
        final String[] milestones = new String[mAllMilestone.size() + 1];
        
        milestones[0] = getResources().getString(R.string.issue_clear_milestone);
        
        int checkedItem = 0;
        if (mSelectedMilestone != null) {
            checkedItem = mSelectedMilestone.getNumber();
        }
        
        for (int i = 1; i <= mAllMilestone.size(); i++) {
            Milestone m = mAllMilestone.get(i - 1);
            milestones[i] = m.getTitle();
            if (m.getNumber() == checkedItem) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = createDialogBuilder();
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_milestone);
        builder.setSingleChoiceItems(milestones, checkedItem, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mSelectedMilestone = null;
                }
                else {
                    mSelectedMilestone = mAllMilestone.get(which - 1);
                }
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (mSelectedMilestone != null) {
                    mTvSelectedMilestone.setText(getString(
                            R.string.issue_milestone, mSelectedMilestone.getTitle()));
                }
                else {
                    mTvSelectedMilestone.setText(null);
                }
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    private void showAssigneesDialog() {
        final String[] assignees = new String[mAllAssignee.size() + 1];
        assignees[0] = getResources().getString(R.string.issue_clear_assignee);
        
        int checkedItem = 0;
        
        for (int i = 1; i <= mAllAssignee.size(); i++) {
            User u = mAllAssignee.get(i - 1);
            assignees[i] = u.getLogin();
            if (mSelectedAssignee != null
                    && u.getLogin().equalsIgnoreCase(mSelectedAssignee.getLogin())) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = createDialogBuilder();
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_assignee);
        builder.setSingleChoiceItems(assignees, checkedItem, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mSelectedAssignee = null;
                }
                else {
                    mSelectedAssignee = mAllAssignee.get(which - 1);
                }
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (mSelectedAssignee != null) {
                    mTvSelectedAssignee.setText(getString(
                            R.string.issue_assignee, mSelectedAssignee.getLogin()));
                }
                else {
                    mTvSelectedAssignee.setText(null);
                }
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    public void fillLabels() {
        final Gh4Application app = Gh4Application.get(this);
        
        for (final Label label : mAllLabel) {
            final View rowView = getLayoutInflater().inflate(R.layout.row_issue_create_label, null);
            View viewColor = (View) rowView.findViewById(R.id.view_color);
            viewColor.setBackgroundColor(Color.parseColor("#" + label.getColor()));
            //viewColor.setPadding(10, 10, 10, 10);
            
            final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
            tvLabel.setTypeface(app.condensed);
            tvLabel.setText(label.getName());
            tvLabel.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    if (mSelectedLabels.contains(label)) {
                        mSelectedLabels.remove(label);
                        
                        tvLabel.setTypeface(app.condensed);
                        tvLabel.setBackgroundColor(0);
                        if (Gh4Application.THEME == R.style.LightTheme) {
                            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
                        }
                        else {
                            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));                            
                        }
                    }
                    else {
                        mSelectedLabels.add(label);
                        
                        tvLabel.setTypeface(app.boldCondensed);
                        tvLabel.setBackgroundColor(Color.parseColor("#" + label.getColor()));
                        int r = Color.red(Color.parseColor("#" + label.getColor()));
                        int g = Color.green(Color.parseColor("#" + label.getColor()));
                        int b = Color.blue(Color.parseColor("#" + label.getColor()));
                        if (r + g + b < 383) {
                            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));
                        }
                        else {
                            tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
                        }               
                    }
                }
            });
            
            if (mEditMode) {
                if (mSelectedLabels.contains(label)) {
                    tvLabel.setTypeface(app.boldCondensed);
                    tvLabel.setBackgroundColor(Color.parseColor("#" + label.getColor()));
                    int r = Color.red(Color.parseColor("#" + label.getColor()));
                    int g = Color.green(Color.parseColor("#" + label.getColor()));
                    int b = Color.blue(Color.parseColor("#" + label.getColor()));
                    if (r + g + b < 383) {
                        tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));
                    }
                    else {
                        tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
                    }           
                }
                else {
                    tvLabel.setTypeface(app.condensed);
                    tvLabel.setBackgroundColor(0);
                    if (Gh4Application.THEME == R.style.LightTheme) {
                        tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
                    }
                    else {
                        tvLabel.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_dark));                         
                    }
                }
            }
            
            mLinearLayoutLabels.addView(rowView);
        }
    }
    
    private void fillIssueData() {
        EditText etTitle = (EditText) findViewById(R.id.et_title);
        etTitle.setText(mEditIssue.getTitle());
        
        EditText etDesc = (EditText) findViewById(R.id.et_desc);
        etDesc.setText(mEditIssue.getBody());
        
        mSelectedLabels = new ArrayList<Label>();
        mSelectedLabels.addAll(mEditIssue.getLabels());
        
        mSelectedMilestone = mEditIssue.getMilestone();
        
        mSelectedAssignee = mEditIssue.getAssignee();
        
        if (mSelectedMilestone != null) {
            mTvSelectedMilestone.setText(getString(
                    R.string.issue_milestone, mEditIssue.getMilestone().getTitle()));
        }
        
        if (mSelectedAssignee != null) {
            mTvSelectedAssignee.setText(getString(
                    R.string.issue_assignee, mSelectedAssignee.getLogin()));
        }
    }

    private boolean checkForError(LoaderResult<?> result) {
        if (isLoaderError(result)) {
            hideLoading();
            stopProgressDialog(mProgressDialog);
            return true;
        }
        return false;
    }
}
