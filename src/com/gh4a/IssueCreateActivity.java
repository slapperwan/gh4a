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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.gh4a.adapter.MilestoneSimpleAdapter;
import com.gh4a.db.Bookmark;
import com.gh4a.db.BookmarkParam;
import com.gh4a.db.DbHelper;
import com.gh4a.holder.BreadCrumbHolder;
import com.gh4a.utils.StringUtils;

/**
 * The IssueCreate activity.
 */
public class IssueCreateActivity extends BaseActivity implements OnClickListener, OnItemSelectedListener {

    /** The user login. */
    private String mUserLogin;

    /** The repo name. */
    private String mRepoName;
    
    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;
    
    protected List<Label> mSelectedLabels;
    
    protected Milestone mSelectedMilestone;
    
    protected List<Label> mLabels;
    
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
        setContentView(R.layout.issue_create);
        setUpActionBar();
        
        mUserLogin = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        setBreadCrumb();
        
        Button btnCreate = (Button) findViewById(R.id.btn_create);
        btnCreate.setOnClickListener(this);
        
        Button btnLabel = (Button) findViewById(R.id.btn_label);
        btnLabel.setOnClickListener(this);
        
        new LoadIssueLabelsTask(this).execute();
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

        createBreadcrumb("Create Issue", breadCrumbHolders);
    }

    /* (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_create) {
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
                new CreateIssueTask(this, false).execute(etTitle.getText().toString(), desc);
            }
        }
        else if (v.getId() == R.id.btn_label) {
            showLabelsDialog();
        }
    }

    /**
     * An asynchronous task that runs on a background thread
     * to create issue.
     */
    private static class CreateIssueTask extends AsyncTask<String, Void, Boolean> {

        /** The target. */
        private WeakReference<IssueCreateActivity> mTarget;
        
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
        public CreateIssueTask(IssueCreateActivity activity, boolean hideMainView) {
            mTarget = new WeakReference<IssueCreateActivity>(activity);
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
                    IssueCreateActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    IssueService issueService = new IssueService(client);
                    
                    Issue issue = new Issue();
                    issue.setTitle(params[0]);
                    issue.setBody(params[1]);
                    
                    if (activity.mSelectedMilestone != null
                            && activity.mSelectedMilestone.getNumber() != -1) {
                        issue.setMilestone(activity.mSelectedMilestone);
                    }
                    if (activity.mSelectedLabels != null) {
                        issue.setLabels(activity.mSelectedLabels);
                    }
                    
                    issueService.createIssue(activity.mUserLogin, 
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
                IssueCreateActivity activity = mTarget.get();
                activity.mLoadingDialog.dismiss();
    
                if (mException) {
                    activity.showError(false);
                }
                else {
                    activity.showMessage(activity.getResources().getString(R.string.issue_success_create),
                            false);
                    activity.getApplicationContext().openIssueListActivity(activity, 
                            activity.mUserLogin, 
                            activity.mRepoName, 
                            Constants.Issue.ISSUE_STATE_OPEN);
                }
            }
        }
    }
    
    /**
     * An asynchronous task that runs on a background thread
     * to load issue labels.
     */
    private static class LoadIssueLabelsTask extends AsyncTask<Void, Integer, Map<String, Object>> {

        /** The target. */
        private WeakReference<IssueCreateActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load issue task.
         *
         * @param activity the activity
         */
        public LoadIssueLabelsTask(IssueCreateActivity activity) {
            mTarget = new WeakReference<IssueCreateActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected Map<String, Object> doInBackground(Void... params) {
            if (mTarget.get() != null) {
                try {
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    LabelService labelService = new LabelService(client);
                    List<Label> labels = labelService.getLabels(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName);
                    
                    MilestoneService milestoneService = new MilestoneService(client);
                    List<Milestone> milestones = milestoneService.getMilestones(mTarget.get().mUserLogin,
                            mTarget.get().mRepoName, null);
                    
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("labels", labels);
                    m.put("milestones", milestones);
                    
                    return m;
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
        protected void onPostExecute(Map<String, Object> result) {
            if (mTarget.get() != null) {
                IssueCreateActivity activity = mTarget.get();
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
    
    public void fillData(Map<String, Object> labelsAndMilestones) {
        Spinner milestoneSpinner = (Spinner) findViewById(R.id.spinner_milestone);
        
        mLabels = (List<Label>) labelsAndMilestones.get("labels");
        List<Milestone> milestones = (List<Milestone>) labelsAndMilestones.get("milestones");
        
        List<Milestone> finalMilestones = new ArrayList<Milestone>();
        
        Milestone selectMilestone = new Milestone();
        selectMilestone.setNumber(-1);
        selectMilestone.setTitle("Select Milestone");
        finalMilestones.add(selectMilestone);
        finalMilestones.addAll(milestones);
        
        MilestoneSimpleAdapter milestoneAdapter = new MilestoneSimpleAdapter(this, finalMilestones);
        milestoneSpinner.setAdapter(milestoneAdapter);
        milestoneSpinner.setOnItemSelectedListener(this);
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
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(IssueCreateActivity.this, android.R.style.Theme));
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.bookmark_menu, menu);
        }
        return true;
    }
    
    @Override
    public void openBookmarkActivity() {
        Intent intent = new Intent().setClass(this, BookmarkListActivity.class);
        intent.putExtra(Constants.Bookmark.NAME, "Create issue at " + mUserLogin + "/" + mRepoName);
        intent.putExtra(Constants.Bookmark.OBJECT_TYPE, Constants.Bookmark.OBJECT_TYPE_ISSUE);
        startActivityForResult(intent, 100);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
           if (resultCode == Constants.Bookmark.ADD) {
               DbHelper db = new DbHelper(this);
               Bookmark b = new Bookmark();
               b.setName("Create issue at " + mUserLogin + "/" + mRepoName);
               b.setObjectType(Constants.Bookmark.OBJECT_TYPE_ISSUE);
               b.setObjectClass(IssueCreateActivity.class.getName());
               long id = db.saveBookmark(b);
               
               BookmarkParam[] params = new BookmarkParam[2];
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
               
               db.saveBookmarkParam(params);
           }
        }
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
