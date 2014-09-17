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

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.Loader;
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
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.MilestoneService;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class IssueMilestoneEditActivity extends LoadingFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    private int mMilestoneNumber;
    private Milestone mMilestone;
    private Date mDueOn;

    private EditText mTitleView;
    private EditText mDescriptionView;
    private EditText mDueView;

    private LoaderCallbacks<Milestone> mMilestoneCallback = new LoaderCallbacks<Milestone>() {
        @Override
        public Loader<LoaderResult<Milestone>> onCreateLoader(int id, Bundle args) {
            return new MilestoneLoader(IssueMilestoneEditActivity.this,
                    mRepoOwner, mRepoName, mMilestoneNumber);
        }
        @Override
        public void onResultReady(LoaderResult<Milestone> result) {
            boolean success = !result.handleError(IssueMilestoneEditActivity.this);
            if (success) {
                mMilestone = result.getData();
                mTitleView.setText(mMilestone.getTitle());
                mDescriptionView.setText(mMilestone.getDescription());
                mDueOn = mMilestone.getDueOn();
                updateDueDate();
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.NAME);
        mMilestoneNumber = getIntent().getExtras().getInt(Constants.Milestone.NUMBER);

        if (hasErrorView()) {
            return;
        }

        if (!Gh4Application.get(this).isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.issue_create_milestone);
        mTitleView = (EditText) findViewById(R.id.et_title);
        mDescriptionView = (EditText) findViewById(R.id.et_desc);
        mDueView = (EditText) findViewById(R.id.et_due_date);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(isInEditMode()
                ? R.string.issue_milestone_edit : R.string.issue_milestone_new);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (isInEditMode()) {
            getSupportLoaderManager().initLoader(0, null, mMilestoneCallback);
        }
        setContentShown(!isInEditMode());
    }

    private boolean isInEditMode() {
        return mMilestoneNumber != 0;
    }

    private void openIssueMilestones() {
        Intent intent = new Intent(this, IssueMilestoneListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(isInEditMode() ? R.menu.accept_delete : R.menu.accept_cancel, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openIssueListActivity(this, mRepoOwner, mRepoName,
                Constants.Issue.STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.accept: {
                String title = mTitleView.getText() == null
                        ? null : mTitleView.getText().toString();
                if (StringUtils.isBlank(title)) {
                    ToastUtils.showMessage(this, R.string.issue_error_milestone_title);
                } else {
                    String desc = null;
                    if (mDescriptionView.getText() != null) {
                        desc = mDescriptionView.getText().toString();
                    }
                    new SaveIssueMilestoneTask(title, desc, mDueOn).execute();
                }
                return true;
            }
            case R.id.delete:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.issue_dialog_delete_title,
                                mMilestone.getTitle()))
                        .setMessage(R.string.issue_dialog_delete_message)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                new DeleteIssueMilestoneTask(mMilestone.getNumber()).execute();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return true;
            case R.id.cancel:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showDatePickerDialog(View view) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getSupportFragmentManager(), "datePicker");
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
        updateDueDate();
    }

    private void updateDueDate() {
        if (mDueOn != null) {
            mDueView.setText(getString(R.string.issue_milestone_due,
                    DateFormat.getMediumDateFormat(this).format(mDueOn)));
        } else {
            mDueView.setText(null);
        }
    }

    private class SaveIssueMilestoneTask extends ProgressDialogTask<Void> {
        private String mTitle;
        private String mDesc;
        private Date mDueOn;

        public SaveIssueMilestoneTask(String title, String desc, Date dueOn) {
            super(IssueMilestoneEditActivity.this, 0, R.string.saving_msg);
            mTitle = title;
            mDesc = desc;
            mDueOn = dueOn;
        }

        @Override
        protected Void run() throws IOException {
            MilestoneService milestoneService = (MilestoneService)
                    Gh4Application.get(mContext).getService(Gh4Application.MILESTONE_SERVICE);

            Milestone milestone = isInEditMode() ? mMilestone : new Milestone();
            milestone.setTitle(mTitle);
            milestone.setDescription(mDesc);
            milestone.setDueOn(mDueOn);

            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            if (isInEditMode()) {
                milestoneService.editMilestone(repoId, milestone);
            } else {
                milestoneService.createMilestone(repoId, milestone);
            }

            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            openIssueMilestones();
        }

        @Override
        protected void onError(Exception e) {
            ToastUtils.showMessage(mContext, R.string.issue_error_create_milestone);
        }
    }

    private class DeleteIssueMilestoneTask extends ProgressDialogTask<Void> {
        private int mNumber;

        public DeleteIssueMilestoneTask(int number) {
            super(IssueMilestoneEditActivity.this, 0, R.string.deleting_msg);
            mNumber = number;
        }

        @Override
        protected Void run() throws IOException {
            MilestoneService milestoneService = (MilestoneService)
                    Gh4Application.get(mContext).getService(Gh4Application.MILESTONE_SERVICE);
            milestoneService.deleteMilestone(mRepoOwner, mRepoName, mNumber);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            openIssueMilestones();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final IssueMilestoneEditActivity activity = (IssueMilestoneEditActivity) getActivity();
            final Calendar c = Calendar.getInstance();

            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            if (activity.mDueOn != null) {
                c.setTime(activity.mDueOn);
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
}
