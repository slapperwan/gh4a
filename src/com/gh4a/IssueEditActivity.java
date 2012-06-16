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
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.LabelService;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.gh4a.adapter.MilestoneSimpleAdapter;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;

/**
 * The IssueEdit activity.
 */
public class IssueEditActivity extends BaseActivity implements OnClickListener, OnItemSelectedListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The issue number. */
    private int mIssueNumber;
    
    private String mTitle;
    
    private String mBody;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    protected List<Label> mSelectedLabels;
    
    protected Milestone mSelectedMilestone;
    
    protected List<Label> mLabels;
    
    protected List<Milestone> mMilestones;
    
    protected Issue mIssue;
    
    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_edit);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mIssueNumber = getIntent().getExtras().getInt(Constants.Issue.ISSUE_NUMBER);
        mTitle = getIntent().getExtras().getString(Constants.Issue.ISSUE_TITLE);
        mBody = getIntent().getExtras().getString(Constants.Issue.ISSUE_BODY);

        setBreadCrumb();
        
        new LoadIssueTask(this).execute();
    }
    
    /**
     * Sets the bread crumb.
     */
    protected void setBreadCrumb() {
        BreadCrumbHolder[] breadCrumbHolders = new BreadCrumbHolder[4];

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
        
        // Issue
        b = new BreadCrumbHolder();
        b.setLabel("Issue #" + mIssueNumber);
        b.setTag(Constants.Issue.ISSUE);
        data.put(Constants.Issue.ISSUE_NUMBER, String.valueOf(mIssueNumber));
        b.setData(data);
        breadCrumbHolders[3] = b;

        createBreadcrumb("Edit Issue #" + mIssueNumber, breadCrumbHolders);
    }

    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_edit) {
            EditText etTitle = (EditText) findViewById(R.id.et_title);
            EditText etDesc = (EditText) findViewById(R.id.et_desc);
            if (etTitle.getText() == null || StringUtils.isBlank(etTitle.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_title), false);
            }
            else {
                String desc = "";
                if (etTitle.getText() != null) {
                    desc = etDesc.getText().toString();
                }
                new EditIssueTask(this, false).execute(String.valueOf(mIssueNumber), etTitle.getText().toString(), desc);
            }
        }
        else if (v.getId() == R.id.btn_label) {
            showLabelsDialog();
        }
    }

    /**
     * An asynchronous task that runs on a background thread to load issue.
     */
    private static class LoadIssueTask extends AsyncTask<Void, Integer, Void> {

        /** The target. */
        private WeakReference<IssueEditActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue task.
         *
         * @param activity the activity
         */
        public LoadIssueTask(IssueEditActivity activity) {
            mTarget = new WeakReference<IssueEditActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Void doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    IssueEditActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(activity.getAuthToken());
                    IssueService issueService = new IssueService(client);
                    activity.mIssue = issueService.getIssue(
                            activity.mUserLogin, activity.mRepoName, 
                            activity.mIssueNumber);
                    
                    activity.mSelectedLabels = activity.mIssue.getLabels(); 
                    
                    LabelService labelService = new LabelService(client);
                    activity.mLabels = labelService.getLabels(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName);
                    
                    MilestoneService milestoneService = new MilestoneService(client);
                    activity.mMilestones = milestoneService.getMilestones(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName, null);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                }
            }
            else {
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
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueEditActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.fillIssueData();
                }
            }
        }
    }
    
    public void fillIssueData() {
        EditText etTitle = (EditText) findViewById(R.id.et_title);
        etTitle.setText(mIssue.getTitle());
        
        EditText etDesc = (EditText) findViewById(R.id.et_desc);
        etDesc.setText(mIssue.getBody());
        
        Button btnEdit = (Button) findViewById(R.id.btn_edit);
        btnEdit.setOnClickListener(this);
        
        Button btnLabel = (Button) findViewById(R.id.btn_label);
        btnLabel.setOnClickListener(this);
        
        fillLabelMilestoneData();
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to edit issue.
     */
    private static class EditIssueTask extends AsyncTask<String, Void, Boolean> {

        /** The target. */
        private WeakReference<IssueEditActivity> mTarget;
        
        /** The exception. */
        private boolean mException;
        
        /** The hide main view. */
        private boolean mHideMainView;

        /**
         * Instantiates a new load issue list task.
         *
         * @param activity the activity
         * @param hideMainView the hide main view
         */
        public EditIssueTask(IssueEditActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueEditActivity>(activity);
            mHideMainView = hideMainView;
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Boolean doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueEditActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    IssueService issueService = new IssueService(client);
                    
                    Issue issue = activity.mIssue;
                    
                    issue.setTitle(params[1]);
                    issue.setBody(params[2]);
                    
                    issue.setMilestone(activity.mSelectedMilestone);
                    
                    if (activity.mSelectedLabels != null) {
                        issue.setLabels(activity.mSelectedLabels);
                    }
                    
                    issueService.editIssue(activity.mUserLogin, 
                            activity.mRepoName, issue);
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
        protected void onPostExecute(Boolean result) {
            if (mTarget.get() != null) {
                IssueEditActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showMessage(activity.getResources().getString(R.string.issue_error_edit),
                            false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_edit),
                            false);
                    activity.getApplicationContext().openIssueActivity(activity, 
                            activity.mUserLogin, 
                            activity.mRepoName, 
                            activity.mIssueNumber, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                }
            }
        }
    }
    
    public void fillLabelMilestoneData() {
        Spinner milestoneSpinner = (Spinner) findViewById(R.id.spinner_milestone);
        
        List<Milestone> finalMilestones = new ArrayList<Milestone>();
        
        Milestone selectMilestone = new Milestone();
        selectMilestone.setNumber(-1);
        selectMilestone.setTitle("Select Milestone");
        finalMilestones.add(selectMilestone);
        finalMilestones.addAll(mMilestones);
        
        MilestoneSimpleAdapter milestoneAdapter = new MilestoneSimpleAdapter(this, finalMilestones);
        milestoneSpinner.setAdapter(milestoneAdapter);
        milestoneSpinner.setOnItemSelectedListener(this);
        
        if (mIssue.getMilestone() != null) {
            for (int i = 0; i < finalMilestones.size(); i++) {
                Milestone m = finalMilestones.get(i);
                if (m.getNumber() == mIssue.getMilestone().getNumber()) {
                    milestoneSpinner.setSelection(i);
                }
            }
        }
        
        String btnText = "Labels : ";
        for (Label l : mSelectedLabels) {
            btnText += l.getName() + ", ";
        }
        btnText = btnText.substring(0, btnText.length() - 2);
        Button btnLabel = (Button) findViewById(R.id.btn_label);
        btnLabel.setText(btnText);
    }

    private void showLabelsDialog() {
        final boolean[] checkedItems = new boolean[mLabels.size()];
        final String[] availabelLabelArr = new String[mLabels.size()];
        
        List<String> currentSelected = new ArrayList<String>();
        if (mSelectedLabels != null) {
            for (Label l : mSelectedLabels) {
                currentSelected.add(l.getName());
            }
        }
        for (int i = 0; i < mLabels.size(); i++) {
            availabelLabelArr[i] = mLabels.get(i).getName();
            
            if(currentSelected.contains(mLabels.get(i).getName())) {
                checkedItems[i] = true;
            }
            else {
                checkedItems[i] = false;
            }
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(
                new ContextThemeWrapper(IssueEditActivity.this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_labels);
        builder.setMultiChoiceItems(availabelLabelArr, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                if (isChecked) {
                    checkedItems[whichButton] = true;
                }
                else {
                    checkedItems[whichButton] = false;
                }
            }
        });
        
        builder.setPositiveButton(R.string.label_it,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
                mSelectedLabels = new ArrayList<Label>();
                
                String btnText = "Labels : ";
                for (int i = 0; i < checkedItems.length; i++) {
                    if (checkedItems[i]) {
                        btnText += mLabels.get(i).getName() + ", ";
                        mSelectedLabels.add(mLabels.get(i));
                    }
                }
                btnText = btnText.substring(0, btnText.length() - 2);
                Button btnLabel = (Button) findViewById(R.id.btn_label);
                btnLabel.setText(btnText);
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    @Override
    public void onItemSelected(AdapterView<?> adapterView,
            View view, int position, long id) {
        
        MilestoneSimpleAdapter adapter = (MilestoneSimpleAdapter) adapterView.getAdapter();
        mSelectedMilestone = (Milestone) adapter.getItem(position);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
        
    }
}
