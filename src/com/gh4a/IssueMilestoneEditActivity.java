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
import java.util.Calendar;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneLoader;
import com.gh4a.utils.StringUtils;

public class IssueMilestoneEditActivity extends BaseSherlockFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    private int mMilestoneNumber;
    private Milestone mMilestone;
    private ProgressDialog mProgressDialog;

    private LoaderCallbacks<Milestone> mMilestoneCallback = new LoaderCallbacks<Milestone>() {
        @Override
        public Loader<LoaderResult<Milestone>> onCreateLoader(int id, Bundle args) {
            return new MilestoneLoader(IssueMilestoneEditActivity.this,
                    mRepoOwner, mRepoName, mMilestoneNumber);
        }
        @Override
        public void onResultReady(LoaderResult<Milestone> result) {
            hideLoading();

            if (!isLoaderError(result)) {
                mMilestone = result.getData();
                fillData();
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);
        mMilestoneNumber = getIntent().getExtras().getInt(Constants.Milestone.NUMBER);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        if (!isAuthorized()) {
            Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
        }
        setContentView(R.layout.issue_create_milestone);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_milestone_edit);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();
        getSupportLoaderManager().initLoader(0, null, mMilestoneCallback);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }
    
    private static class EditIssueMilestonesTask extends AsyncTask<String, Void, Void> {

        private WeakReference<IssueMilestoneEditActivity> mTarget;
        private boolean mException;
        
        public EditIssueMilestonesTask(IssueMilestoneEditActivity activity) {
            mTarget = new WeakReference<IssueMilestoneEditActivity>(activity);
        }

        @Override
        protected Void doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneEditActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    
                    String title = params[0];
                    String desc = params[1];
                    
                    activity.mMilestone.setTitle(title);
                    activity.mMilestone.setDescription(desc);
                    
                    milestoneService.editMilestone(new RepositoryId(activity.mRepoOwner, activity.mRepoName), activity.mMilestone);
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
            if (mTarget.get() != null) {
                IssueMilestoneEditActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.saving_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneEditActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                
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
        inflater.inflate(R.menu.accept_delete, menu);
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.content_discard_dark);
            menu.getItem(1).setIcon(R.drawable.navigation_accept_dark);
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        final EditText tvTitle = (EditText) findViewById(R.id.et_title);
        final EditText tvDesc = (EditText) findViewById(R.id.et_desc);
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName,
                    Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if (itemId == R.id.accept) {
            String desc = null;
            if (tvDesc.getText() != null) {
                desc = tvDesc.getText().toString();    
            }
            if (tvTitle.getText() == null || StringUtils.isBlank(tvTitle.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_milestone_title), false);
            }
            else {
                new EditIssueMilestonesTask(IssueMilestoneEditActivity.this).execute(tvTitle.getText().toString(), desc);
            }
        } else if (itemId == R.id.delete) {
            AlertDialog.Builder builder = createDialogBuilder();
            builder.setTitle(getString(R.string.issue_dialog_delete_title, mMilestone.getTitle()));
            builder.setMessage(R.string.issue_dialog_delete_message);
            builder.setPositiveButton(R.string.ok,
                    new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    new DeleteIssueMilestoneTask(IssueMilestoneEditActivity.this).execute();
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
        return true;
    }
    
    private static class DeleteIssueMilestoneTask extends AsyncTask<Integer, Void, Void> {

        private WeakReference<IssueMilestoneEditActivity> mTarget;
        private boolean mException;
        
        public DeleteIssueMilestoneTask(IssueMilestoneEditActivity activity) {
            mTarget = new WeakReference<IssueMilestoneEditActivity>(activity);
        }

        @Override
        protected Void doInBackground(Integer... params) {
            if (mTarget.get() != null) {
                try {
                    IssueMilestoneEditActivity activity = mTarget.get();
                    GitHubClient client = new GitHubClient();
                    client.setOAuth2Token(mTarget.get().getAuthToken());
                    MilestoneService milestoneService = new MilestoneService(client);
                    milestoneService.deleteMilestone(activity.mRepoOwner, activity.mRepoName, 
                            activity.mMilestone.getNumber());
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
            if (mTarget.get() != null) {
                IssueMilestoneEditActivity activity = mTarget.get();
                activity.mProgressDialog = activity.showProgressDialog(activity.getString(R.string.deleting_msg), false);
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            if (mTarget.get() != null) {
                IssueMilestoneEditActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                
                if (mException) {
                    activity.showError();
                }
                else {
                    activity.openIssueMilestones();
                }
            }
        }
    }
    
    private void fillData() {
        EditText tvTitle = (EditText) findViewById(R.id.et_title);
        EditText tvDesc = (EditText) findViewById(R.id.et_desc);
        EditText etDueDate = (EditText) findViewById(R.id.et_due_date);
        
        tvTitle.setText(mMilestone.getTitle());
        tvDesc.setText(mMilestone.getDescription());
        
        if (mMilestone.getDueOn() != null) {
            etDueDate.setText(DateFormat.getMediumDateFormat(this).format(mMilestone.getDueOn()));
        }
    }

    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
    
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final IssueMilestoneEditActivity activity = (IssueMilestoneEditActivity) getActivity();
            final Calendar c = Calendar.getInstance();
            
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            
            if (activity.mMilestone.getDueOn() != null) {
                c.setTime(activity.mMilestone.getDueOn());
                year = c.get(Calendar.YEAR);
                month = c.get(Calendar.MONTH);
                day = c.get(Calendar.DAY_OF_MONTH);
            }
            return new DatePickerDialog(activity, this, year, month, day);
        }
        
        public void onDateSet(DatePicker view, int year, int month, int day) {
            final IssueMilestoneEditActivity activity = (IssueMilestoneEditActivity) getActivity();
            activity.setDueOn(year, month, day);
        }
    }
    
    private void setDueOn(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        
        mMilestone.setDueOn(cal.getTime());
        
        EditText etDueDate = (EditText) findViewById(R.id.et_due_date);
        etDueDate.setText(DateFormat.getMediumDateFormat(this).format(mMilestone.getDueOn()));
    }
}
