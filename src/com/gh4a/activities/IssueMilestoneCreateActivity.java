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
import java.util.Calendar;
import java.util.Date;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.service.MilestoneService;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.utils.StringUtils;

public class IssueMilestoneCreateActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    private Date mDueOn;
    
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
        setContentView(R.layout.issue_create_milestone);

        hideLoading();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issue_milestone_new);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
    
    private class AddIssueMilestonesTask extends ProgressDialogTask<Void> {
        private String mTitle;
        private String mDesc;
        private Date mDueOn;
        
        public AddIssueMilestonesTask(String title, String desc, Date dueOn) {
            super(IssueMilestoneCreateActivity.this, 0, R.string.saving_msg);
            mTitle = title;
            mDesc = desc;
            mDueOn = dueOn;
        }

        @Override
        protected Void run() throws IOException {
            MilestoneService milestoneService = (MilestoneService)
                    getApplicationContext().getSystemService(Gh4Application.MILESTONE_SERVICE);
                    
            Milestone milestone = new Milestone();
            milestone.setTitle(mTitle);
            milestone.setDescription(mDesc);
            milestone.setDueOn(mDueOn);
                    
            milestoneService.createMilestone(mRepoOwner, mRepoName, milestone);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            openIssueMilestones();
        }

        @Override
        protected void onError(Exception e) {
            showMessage(getString(R.string.issue_error_create_milestone), false);
        }    
    }
    
    public void showDatePickerDialog(View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }
    
    public static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);
            
            return new DatePickerDialog(getActivity(), this, year, month, day);
        }
        
        public void onDateSet(DatePicker view, int year, int month, int day) {
            IssueMilestoneCreateActivity activity = (IssueMilestoneCreateActivity) getActivity();
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
        
        mDueOn = cal.getTime();
        
        EditText etDueDate = (EditText) findViewById(R.id.et_due_date);
        etDueDate.setText(DateFormat.getMediumDateFormat(this).format(mDueOn));
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
            final EditText tvTitle = (EditText) findViewById(R.id.et_title);
            final EditText tvDesc = (EditText) findViewById(R.id.et_desc);

            String desc = null;
            if (tvDesc.getText() != null) {
                desc = tvDesc.getText().toString();    
            }
            if (tvTitle.getText() == null || StringUtils.isBlank(tvTitle.getText().toString())) {
                showMessage(getResources().getString(R.string.issue_error_milestone_title), false);
            }
            else {
                new AddIssueMilestonesTask(tvTitle.getText().toString(), desc, mDueOn).execute();
            }
            return true;
        case R.id.cancel:
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
