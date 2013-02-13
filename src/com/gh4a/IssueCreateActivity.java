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
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants.LoaderResult;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.StringUtils;

public class IssueCreateActivity extends BaseSherlockFragmentActivity 
    implements LoaderManager.LoaderCallbacks {

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
    private boolean isCollaborator;
    private ProgressDialog mProgressDialog;
    
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
        mActionBar.setTitle(mEditMode ? 
                getResources().getString(R.string.issue_edit)  + " #" + mIssueNumber 
                : getResources().getString(R.string.issue_create));
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        mEtTitle = (EditText) findViewById(R.id.et_title);
        mEtDesc = (EditText) findViewById(R.id.et_desc);
        
        mLinearLayoutLabels = (LinearLayout) findViewById(R.id.ll_labels);
        mTvSelectedMilestone = (EditText) findViewById(R.id.et_milestone);
        
        mTvSelectedAssignee = (EditText) findViewById(R.id.et_assignee);
        
        TextView tvIssueLabelAdd = (TextView) findViewById(R.id.tv_issue_label_add);
        tvIssueLabelAdd.setTypeface(getApplicationContext().boldCondensed);
        tvIssueLabelAdd.setTextColor(Color.parseColor("#0099cc"));
        
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
        getSupportLoaderManager().initLoader(4, null, this);
        
        LinearLayout ll = (LinearLayout) findViewById(R.id.for_collaborator);
        ll.setVisibility(View.GONE);
        
        if (mEditMode) {
            showLoading();
            getSupportLoaderManager().initLoader(3, null, this);
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
    public boolean setMenuOptionItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.accept) {
            if (mEtTitle.getText() == null || StringUtils.isBlank(mEtTitle.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_title), false);
            }
            else {
                new SaveIssueTask(this, false).execute();
            }
        } else if (itemId == R.id.cancel) {
            finish();
        }
        return true;
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
                    activity.getApplicationContext().openIssueActivity(activity, 
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
        
        int dialogTheme = Gh4Application.THEME == R.style.DefaultTheme ? 
                R.style.Theme_Sherlock_Dialog : R.style.Theme_Sherlock_Light_Dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
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
                    mTvSelectedMilestone.setText(IssueCreateActivity.this.getResources().getString(R.string.issue_milestone)
                            + " : "
                            + mSelectedMilestone.getTitle());
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
        
        int dialogTheme = Gh4Application.THEME == R.style.DefaultTheme ? 
                R.style.Theme_Sherlock_Dialog : R.style.Theme_Sherlock_Light_Dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, dialogTheme));
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
                    mTvSelectedAssignee.setText(IssueCreateActivity.this.getResources().getString(R.string.issue_assignee)
                            + " : "
                            + mSelectedAssignee.getLogin());
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
        final Typeface boldCondensed = getApplicationContext().boldCondensed;
        final Typeface condensed = getApplicationContext().condensed;
        
        for (final Label label : mAllLabel) {
            final View rowView = getLayoutInflater().inflate(R.layout.row_issue_create_label, null);
            View viewColor = (View) rowView.findViewById(R.id.view_color);
            viewColor.setBackgroundColor(Color.parseColor("#" + label.getColor()));
            //viewColor.setPadding(10, 10, 10, 10);
            
            final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
            tvLabel.setTypeface(condensed);
            tvLabel.setText(label.getName());
            tvLabel.setOnClickListener(new OnClickListener() {
                
                @Override
                public void onClick(View v) {
                    if (mSelectedLabels.contains(label)) {
                        mSelectedLabels.remove(label);
                        
                        tvLabel.setTypeface(condensed);
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
                        
                        tvLabel.setTypeface(boldCondensed);
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
                    tvLabel.setTypeface(boldCondensed);
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
                    tvLabel.setTypeface(condensed);
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
            mTvSelectedMilestone.setText(IssueCreateActivity.this.getResources().getString(R.string.issue_milestone)
                    + " : "
                    + mEditIssue.getMilestone().getTitle());
        }
        
        if (mSelectedAssignee != null) {
            mTvSelectedAssignee.setText(IssueCreateActivity.this.getResources().getString(R.string.issue_assignee)
                    + " : "
                    + mSelectedAssignee.getLogin());
        }
    }
    
    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        if (id == 0) {
            return new LabelListLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 1) {
            return new MilestoneListLoader(this, mRepoOwner, mRepoName, "open");
        }
        else if (id == 2) {
            return new CollaboratorListLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 3) {
            return new IssueLoader(this, mRepoOwner, mRepoName, mIssueNumber);
        }
        else {
            return new IsCollaboratorLoader(this, mRepoOwner, mRepoName);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        HashMap<Integer, Object> result = (HashMap<Integer, Object>) object;
        
        if (!isLoaderError(result)) {
            Object data = result.get(LoaderResult.DATA); 
        
            if (loader.getId() == 0) {
                mAllLabel = (List<Label>) data;
                fillLabels();
            }
            else if (loader.getId() == 1) {
                stopProgressDialog(mProgressDialog);
                mAllMilestone = (List<Milestone>) data;
                showMilestonesDialog();
            }
            else if (loader.getId() == 2) {
                stopProgressDialog(mProgressDialog);
                mAllAssignee = (List<User>) data;
                showAssigneesDialog();
            }
            else if (loader.getId() == 3) {
                hideLoading();
                mEditIssue = (Issue) data;
                getSupportLoaderManager().getLoader(4).forceLoad();
                fillIssueData();
            }
            else {
                isCollaborator = (Boolean) data;
                LinearLayout ll = (LinearLayout) findViewById(R.id.for_collaborator);
                if (isCollaborator) {
                    ll.setVisibility(View.VISIBLE);
                    getSupportLoaderManager().getLoader(0).forceLoad();
                }
                else {
                    ll.setVisibility(View.GONE);
                }
            }
        }
        else {
            hideLoading();
            stopProgressDialog(mProgressDialog);
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }

}
