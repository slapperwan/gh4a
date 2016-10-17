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

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.IssueTemplateLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IssueEditActivity extends BaseActivity implements View.OnClickListener {
    public static final String EXTRA_ISSUE = "issue";

    private String mRepoOwner;
    private String mRepoName;

    private boolean mIsCollaborator;
    private List<Milestone> mAllMilestone;
    private List<User> mAllAssignee;
    private List<Label> mAllLabels;
    private Issue mEditIssue;

    private TextInputLayout mTitleWrapper;
    private EditText mTitleView;
    private TextInputLayout mDescWrapper;
    private EditText mDescView;
    private TextView mTvSelectedMilestone;
    private TextView mTvSelectedAssignee;
    private TextView mTvLabels;

    private View mMilestoneContainer;
    private View mAssigneeContainer;
    private View mLabelContainer;

    private ProgressDialog mProgressDialog;

    private static final String STATE_KEY_ISSUE = "issue";

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>(this) {
        @Override
        protected Loader<LoaderResult<List<Label>>> onCreateLoader() {
            return new LabelListLoader(IssueEditActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(List<Label> result) {
            stopProgressDialog(mProgressDialog);
            mAllLabels = result;
            showLabelDialog();
            getSupportLoaderManager().destroyLoader(0);
        }
        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback = new LoaderCallbacks<List<Milestone>>(this) {
        @Override
        protected Loader<LoaderResult<List<Milestone>>> onCreateLoader() {
            return new MilestoneListLoader(IssueEditActivity.this,
                    mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
        }
        @Override
        protected void onResultReady(List<Milestone> result) {
            stopProgressDialog(mProgressDialog);
            mAllMilestone = result;
            showMilestonesDialog();
            getSupportLoaderManager().destroyLoader(1);
        }
        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback = new LoaderCallbacks<List<User>>(this) {
        @Override
        protected Loader<LoaderResult<List<User>>> onCreateLoader() {
            return new CollaboratorListLoader(IssueEditActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(List<User> result) {
            stopProgressDialog(mProgressDialog);
            mAllAssignee = result;
            showAssigneesDialog();
            getSupportLoaderManager().destroyLoader(2);
        }
        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private LoaderCallbacks<Boolean> mIsCollaboratorCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsCollaboratorLoader(IssueEditActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsCollaborator = result;
            updateLabels();
            updateLabelStates();
        }
    };

    private LoaderCallbacks<String> mIssueTemplateCallback = new LoaderCallbacks<String>(this) {
        @Override
        protected Loader<LoaderResult<String>> onCreateLoader() {
            return new IssueTemplateLoader(IssueEditActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(String result) {
            getSupportLoaderManager().destroyLoader(4);
            mDescWrapper.setHint(getString(R.string.issue_body));
            mDescView.setEnabled(true);
            mDescView.setText(result);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Gh4Application.get().isAuthorized()) {
            Intent intent = new Intent(this, Github4AndroidActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.issue_create);
        setToolbarScrollable(false);

        LayoutInflater headerInflater = LayoutInflater.from(UiUtils.makeHeaderThemedContext(this));
        View header = headerInflater.inflate(R.layout.issue_create_header, null);
        addHeaderView(header, false);

        mTitleWrapper = (TextInputLayout) header.findViewById(R.id.title_wrapper);
        mTitleView = (EditText) header.findViewById(R.id.et_title);
        mDescWrapper = (TextInputLayout) header.findViewById(R.id.desc_wrapper);
        mDescView = (EditText) header.findViewById(R.id.et_desc);

        CoordinatorLayout rootLayout = getRootLayout();
        FloatingActionButton fab = (FloatingActionButton)
                getLayoutInflater().inflate(R.layout.accept_fab, rootLayout, false);
        fab.setOnClickListener(this);
        rootLayout.addView(fab);

        findViewById(R.id.milestone_container).setOnClickListener(this);
        findViewById(R.id.assignee_container).setOnClickListener(this);
        findViewById(R.id.label_container).setOnClickListener(this);

        mTvSelectedMilestone = (TextView) findViewById(R.id.tv_milestone);
        mTvSelectedAssignee = (TextView) findViewById(R.id.tv_assignee);
        mTvLabels = (TextView) findViewById(R.id.tv_labels);

        mMilestoneContainer = findViewById(R.id.milestone_container);
        mAssigneeContainer = findViewById(R.id.assignee_container);
        mLabelContainer = findViewById(R.id.label_container);

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_ISSUE)) {
            mEditIssue = (Issue) savedInstanceState.getSerializable(STATE_KEY_ISSUE);
        } else if (!isInEditMode()) {
            getSupportLoaderManager().initLoader(4, null, mIssueTemplateCallback);
            mDescView.setEnabled(false);
            mDescWrapper.setHint(getString(R.string.issue_loading_template_hint));
        }

        if (mEditIssue == null) {
            mEditIssue = new Issue();
        }

        mTitleView.setText(mEditIssue.getTitle());
        mDescView.setText(mEditIssue.getBody());

        mTitleView.addTextChangedListener(new UiUtils.ButtonEnableTextWatcher(mTitleView, fab));
        mTitleView.addTextChangedListener(new UiUtils.EmptinessWatchingTextWatcher(mTitleView) {
            @Override
            public void onIsEmpty(boolean isEmpty) {
                if (isEmpty) {
                    mTitleWrapper.setError(getString(R.string.issue_error_title));
                } else {
                    mTitleWrapper.setErrorEnabled(false);
                }
            }
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(isInEditMode()
                ? getString(R.string.issue_edit_title, mEditIssue.getNumber())
                : getString(R.string.issue_create));
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        updateLabels();
        updateLabelStates();
        setToolbarScrollable(false);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mEditIssue = (Issue) extras.getSerializable(EXTRA_ISSUE);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // swipe-to-refresh doesn't make much sense in the
        // interaction model of this activity
        return false;
    }

    @Override
    public void onRefresh() {
        mAllAssignee = null;
        mAllMilestone = null;
        mAllLabels = null;
        mIsCollaborator = false;
        updateLabels();
        updateLabelStates();

        LoaderManager lm = getSupportLoaderManager();
        for (int i = 0; i < 4; i++) {
            Loader loader = lm.getLoader(i);
            if (loader != null) {
                loader.onContentChanged();
            }
        }
        super.onRefresh();
    }

    private boolean isInEditMode() {
        return getIntent().hasExtra(EXTRA_ISSUE);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.milestone_container) {
            showMilestonesDialog();
        } else if (id == R.id.assignee_container) {
            showAssigneesDialog();
        } else if (id == R.id.label_container) {
            showLabelDialog();
        } else if (view instanceof FloatingActionButton) {
            mEditIssue.setTitle(mTitleView.getText().toString());
            mEditIssue.setBody(mDescView.getText().toString());
            new SaveIssueTask(mEditIssue).schedule();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mEditIssue != null) {
            outState.putSerializable(STATE_KEY_ISSUE, mEditIssue);
        }
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getIssueListActivityIntent(this,
                mRepoOwner, mRepoName, Constants.Issue.STATE_OPEN);
    }

    private void showMilestonesDialog() {
        if (mAllMilestone == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
        } else {
            final String[] milestones = new String[mAllMilestone.size() + 1];
            Milestone selectedMilestone = mEditIssue.getMilestone();
            int selected = 0;

            milestones[0] = getResources().getString(R.string.issue_clear_milestone);
            for (int i = 1; i <= mAllMilestone.size(); i++) {
                Milestone m = mAllMilestone.get(i - 1);
                milestones[i] = m.getTitle();
                if (selectedMilestone != null && m.getNumber() == selectedMilestone.getNumber()) {
                    selected = i;
                }
            }

            final DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        mEditIssue.setMilestone(null);
                    } else {
                        mEditIssue.setMilestone(mAllMilestone.get(which - 1));
                    }
                    updateLabels();
                    dialog.dismiss();
                }
            };

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_milestone_hint)
                    .setSingleChoiceItems(milestones, selected, selectCb)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void showAssigneesDialog() {
        if (mAllAssignee == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
        } else {
            final String[] assignees = new String[mAllAssignee.size() + 1];
            User selectedAssignee = mEditIssue.getAssignee();
            int selected = 0;

            assignees[0] = getResources().getString(R.string.issue_clear_assignee);

            for (int i = 1; i <= mAllAssignee.size(); i++) {
                User u = mAllAssignee.get(i - 1);
                assignees[i] = u.getLogin();
                if (selectedAssignee != null
                        && u.getLogin().equalsIgnoreCase(selectedAssignee.getLogin())) {
                    selected = i;
                }
            }

            DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which == 0) {
                        mEditIssue.setAssignee(null);
                    } else {
                        mEditIssue.setAssignee(mAllAssignee.get(which - 1));
                    }
                    updateLabels();
                    dialog.dismiss();
                }
            };

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_assignee_hint)
                    .setSingleChoiceItems(assignees, selected, selectCb)
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        }
    }

    private void showLabelDialog() {
        if (mAllLabels == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(0, null, mLabelCallback);
        } else {
            LayoutInflater inflater = getLayoutInflater();
            final List<Label> selectedLabels = mEditIssue.getLabels() != null
                    ? new ArrayList<>(mEditIssue.getLabels()) : new ArrayList<Label>();
            View labelContainerView = inflater.inflate(R.layout.generic_linear_container, null);
            ViewGroup container = (ViewGroup) labelContainerView.findViewById(R.id.container);

            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Label label = (Label) view.getTag();
                    if (selectedLabels.contains(label)) {
                        selectedLabels.remove(label);
                        setLabelSelection((TextView) view, false);
                    } else {
                        selectedLabels.add(label);
                        setLabelSelection((TextView) view, true);
                    }
                }
            };

            for (final Label label : mAllLabels) {
                final View rowView = inflater.inflate(R.layout.row_issue_create_label, container, false);
                View viewColor = rowView.findViewById(R.id.view_color);
                viewColor.setBackgroundColor(ApiHelpers.colorForLabel(label));

                final TextView tvLabel = (TextView) rowView.findViewById(R.id.tv_title);
                tvLabel.setText(label.getName());
                tvLabel.setOnClickListener(clickListener);
                tvLabel.setTag(label);

                setLabelSelection(tvLabel, selectedLabels.contains(label));
                container.addView(rowView);
            }

            new AlertDialog.Builder(this)
                    .setCancelable(true)
                    .setTitle(R.string.issue_labels)
                    .setView(labelContainerView)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mEditIssue.setLabels(selectedLabels);
                            updateLabels();
                        }
                    })
                    .show();
        }
    }

    private void setLabelSelection(TextView view, boolean selected) {
        Label label = (Label) view.getTag();
        if (selected) {
            int color = ApiHelpers.colorForLabel(label);
            view.setTypeface(view.getTypeface(), Typeface.BOLD);
            view.setBackgroundColor(color);
            view.setTextColor(UiUtils.textColorForBackground(this, color));
        } else {
            view.setTypeface(view.getTypeface(), 0);
            view.setBackgroundColor(0);
            view.setTextColor(ContextCompat.getColor(this, Gh4Application.THEME != R.style.LightTheme
                    ? R.color.label_fg_light : R.color.label_fg_dark));
        }
    }

    private class SaveIssueTask extends ProgressDialogTask<Void> {
        private Issue mIssue;

        public SaveIssueTask(Issue issue) {
            super(IssueEditActivity.this, 0, R.string.saving_msg);
            mIssue = issue;
        }

        @Override
        protected ProgressDialogTask<Void> clone() {
            return new SaveIssueTask(mIssue);
        }

        @Override
        protected Void run() throws IOException {
            IssueService issueService = (IssueService)
                    Gh4Application.get().getService(Gh4Application.ISSUE_SERVICE);

            if (isInEditMode()) {
                mEditIssue = issueService.editIssue(mRepoOwner, mRepoName, mIssue);
            } else {
                mEditIssue = issueService.createIssue(mRepoOwner, mRepoName, mIssue);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            Intent intent = IntentUtils.getIssueActivityIntent(IssueEditActivity.this,
                    mRepoOwner, mRepoName, mEditIssue.getNumber());
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            setResult(RESULT_OK);
            finish();
        }

        @Override
        protected String getErrorMessage() {
            if (isInEditMode()) {
                return getContext().getString(R.string.issue_error_edit, mIssue.getNumber());
            } else {
                return getContext().getString(R.string.issue_error_create);
            }
        }
    }

    private void updateLabels() {
        if (mEditIssue.getMilestone() != null) {
            mTvSelectedMilestone.setText(mEditIssue.getMilestone().getTitle());
        } else if (!mIsCollaborator && !isInEditMode()) {
            mTvSelectedMilestone.setText(R.string.issue_milestone_collab_only);
        } else {
            mTvSelectedMilestone.setText(R.string.issue_clear_milestone);
        }

        if (mEditIssue.getAssignee() != null) {
            mTvSelectedAssignee.setText(mEditIssue.getAssignee().getLogin());
        } else if (!mIsCollaborator && !isInEditMode()) {
            mTvSelectedAssignee.setText(R.string.issue_assignee_collab_only);
        } else {
            mTvSelectedAssignee.setText(R.string.issue_clear_assignee);
        }

        List<Label> labels = mEditIssue.getLabels();
        if (!mIsCollaborator && !isInEditMode()) {
            mTvLabels.setText(R.string.issue_labels_collab_only);
        } else if (labels == null || labels.isEmpty()) {
            mTvLabels.setText(R.string.issue_no_labels);
        } else {
            StringBuilder labelText = new StringBuilder();
            for (int i = 0; i < labels.size(); i++) {
                if (i != 0) {
                    labelText.append(", ");
                }
                labelText.append(labels.get(i).getName());
            }
            mTvLabels.setText(labelText);
        }
    }

    private void updateLabelStates() {
        mMilestoneContainer.setEnabled(mIsCollaborator);
        findViewById(R.id.tv_milestone_label).setEnabled(mIsCollaborator);
        mTvSelectedMilestone.setEnabled(mIsCollaborator);
        mAssigneeContainer.setEnabled(mIsCollaborator);
        findViewById(R.id.tv_assignee_label).setEnabled(mIsCollaborator);
        mTvSelectedAssignee.setEnabled(mIsCollaborator);
        mLabelContainer.setEnabled(mIsCollaborator);
        findViewById(R.id.tv_labels_label).setEnabled(mIsCollaborator);
        mTvLabels.setEnabled(mIsCollaborator);
    }
}
