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
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.Loader;
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
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IssueCreateActivity extends LoadingFragmentActivity implements OnClickListener {
    private String mRepoOwner;
    private String mRepoName;
    private int mIssueNumber;

    private List<Label> mSelectedLabels;
    private Milestone mSelectedMilestone;
    private User mSelectedAssignee;
    private List<Milestone> mAllMilestone;
    private List<User> mAllAssignee;
    private Issue mEditIssue;

    private ProgressDialog mProgressDialog;
    private TextView mTvSelectedMilestone;
    private TextView mTvSelectedAssignee;

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueCreateActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            stopProgressDialog(mProgressDialog);
            if (!result.handleError(IssueCreateActivity.this)) {
                fillLabels(result.getData());
            }
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback = new LoaderCallbacks<List<Milestone>>() {
        @Override
        public Loader<LoaderResult<List<Milestone>>> onCreateLoader(int id, Bundle args) {
            return new MilestoneListLoader(IssueCreateActivity.this,
                    mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
        }
        @Override
        public void onResultReady(LoaderResult<List<Milestone>> result) {
            stopProgressDialog(mProgressDialog);
            if (!result.handleError(IssueCreateActivity.this)) {
                mAllMilestone = result.getData();
                showMilestonesDialog();
                getSupportLoaderManager().destroyLoader(1);
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
            stopProgressDialog(mProgressDialog);
            if (!result.handleError(IssueCreateActivity.this)) {
                mAllAssignee = result.getData();
                showAssigneesDialog();
                getSupportLoaderManager().destroyLoader(2);
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
            if (!result.handleError(IssueCreateActivity.this)) {
                findViewById(R.id.for_collaborator).setVisibility(result.getData() ? View.VISIBLE : View.GONE);
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
            boolean success = !result.handleError(IssueCreateActivity.this);
            if (success) {
                mEditIssue = result.getData();
                fillIssueData();
            }
            setContentEmpty(!success);
            setContentShown(true);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mSelectedLabels = new ArrayList<Label>();
        Bundle data = getIntent().getExtras();

        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mIssueNumber = data.getInt(Constants.Issue.NUMBER);

        if (hasErrorView()) {
            return;
        }

        if (!Gh4Application.get(this).isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.issue_create);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(isInEditMode() ? getString(R.string.issue_edit_title, mIssueNumber)
                : getString(R.string.issue_create));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        TextView tvIssueLabelAdd = (TextView) findViewById(R.id.tv_issue_label_add);
        tvIssueLabelAdd.setTypeface(Gh4Application.get(this).boldCondensed);
        tvIssueLabelAdd.setTextColor(getResources().getColor(R.color.highlight));

        mTvSelectedMilestone = (TextView) findViewById(R.id.et_milestone);
        mTvSelectedAssignee = (TextView) findViewById(R.id.et_assignee);

        getSupportLoaderManager().initLoader(0, null, mLabelCallback);
        getSupportLoaderManager().initLoader(4, null, mIsCollaboratorCallback);
        if (isInEditMode()) {
            getSupportLoaderManager().initLoader(3, null, mIssueCallback);
        }
        setContentShown(!isInEditMode());
    }

    private boolean isInEditMode() {
        return mIssueNumber != 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.accept_cancel, menu);
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
        case R.id.accept:
            EditText etTitle = (EditText) findViewById(R.id.et_title);
            String title = etTitle.getText() == null ? null : etTitle.getText().toString();
            if (StringUtils.isBlank(title)) {
                etTitle.setError(getString(R.string.issue_error_title));
            } else {
                EditText etDesc = (EditText) findViewById(R.id.et_desc);
                new SaveIssueTask(title, etDesc.getText().toString()).execute();
            }
            return true;
        case R.id.cancel:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void showMilestonesDialog() {
        if (mAllMilestone == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
        } else {
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

            AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.issue_milestone_hint);
            builder.setSingleChoiceItems(milestones, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        mSelectedMilestone = null;
                    } else {
                        mSelectedMilestone = mAllMilestone.get(which - 1);
                    }
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (mSelectedMilestone != null) {
                        mTvSelectedMilestone.setText(getString(
                                R.string.issue_milestone, mSelectedMilestone.getTitle()));
                    } else {
                        mTvSelectedMilestone.setText(null);
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
    }

    public void showAssigneesDialog() {
        if (mAllAssignee == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
        } else {
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

            AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
            builder.setCancelable(true);
            builder.setTitle(R.string.issue_assignee_hint);
            builder.setSingleChoiceItems(assignees, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        mSelectedAssignee = null;
                    } else {
                        mSelectedAssignee = mAllAssignee.get(which - 1);
                    }
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (mSelectedAssignee != null) {
                        mTvSelectedAssignee.setText(getString(
                                R.string.issue_assignee, mSelectedAssignee.getLogin()));
                    } else {
                        mTvSelectedAssignee.setText(null);
                    }
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            builder.show();
        }
    }

    private class SaveIssueTask extends ProgressDialogTask<Void> {
        private String mTitle;
        private String mBody;

        public SaveIssueTask(String title, String body) {
            super(IssueCreateActivity.this, 0, R.string.saving_msg);
            mTitle = title;
            mBody = body;
        }

        @Override
        protected Void run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get(mContext).getService(Gh4Application.ISSUE_SERVICE);

            Issue issue = isInEditMode() ? mEditIssue : new Issue();
            issue.setTitle(mTitle);
            issue.setBody(mBody);

            issue.setLabels(mSelectedLabels);
            issue.setMilestone(mSelectedMilestone);
            issue.setAssignee(mSelectedAssignee);

            if (isInEditMode()) {
                mEditIssue = issueService.editIssue(mRepoOwner, mRepoName, issue);
            } else {
                mEditIssue = issueService.createIssue(mRepoOwner, mRepoName, issue);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            ToastUtils.showMessage(mContext,
                    isInEditMode() ? R.string.issue_success_edit : R.string.issue_success_create);
            IntentUtils.openIssueActivity(IssueCreateActivity.this, mRepoOwner, mRepoName,
                    mEditIssue.getNumber(), Intent.FLAG_ACTIVITY_CLEAR_TOP);
            finish();
        }
    }

    public void fillLabels(List<Label> labels) {
        Gh4Application app = Gh4Application.get(this);
        LinearLayout labelLayout = (LinearLayout) findViewById(R.id.ll_labels);

        for (final Label label : labels) {
            final View rowView = getLayoutInflater().inflate(R.layout.row_issue_create_label, null);
            View viewColor = rowView.findViewById(R.id.view_color);
            viewColor.setBackgroundColor(Color.parseColor("#" + label.getColor()));

            final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
            tvLabel.setTypeface(app.condensed);
            tvLabel.setText(label.getName());
            tvLabel.setOnClickListener(this);
            tvLabel.setTag(label);

            if (isInEditMode()) {
                handleLabelClick(tvLabel, label, mSelectedLabels.contains(label));
            }

            labelLayout.addView(rowView);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getTag() instanceof Label) {
            Label label = (Label) view.getTag();
            TextView tvLabel = (TextView) view;

            if (mSelectedLabels.contains(label)) {
                mSelectedLabels.remove(label);
                handleLabelClick(tvLabel, label, false);
            } else {
                mSelectedLabels.add(label);
                handleLabelClick(tvLabel, label, true);
            }
        }
    }

    private void handleLabelClick(TextView tvLabel, Label label, boolean select) {
        Gh4Application app = Gh4Application.get(this);
        if (!select) {
            mSelectedLabels.remove(label);
            tvLabel.setTypeface(app.condensed);
            tvLabel.setBackgroundColor(0);
            tvLabel.setTextColor(getResources().getColor(Gh4Application.THEME != R.style.LightTheme
                    ? R.color.abs__primary_text_holo_dark : R.color.abs__primary_text_holo_light));
        } else {
            int color = Color.parseColor("#" + label.getColor());

            mSelectedLabels.add(label);
            tvLabel.setTypeface(app.boldCondensed);
            tvLabel.setBackgroundColor(color);
            tvLabel.setTextColor(UiUtils.textColorForBackground(this, color));
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
}