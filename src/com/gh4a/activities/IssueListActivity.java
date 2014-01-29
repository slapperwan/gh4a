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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.text.TextUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.IssueListByCommentsFragment;
import com.gh4a.fragment.IssueListBySubmittedFragment;
import com.gh4a.fragment.IssueListByUpdatedFragment;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

public class IssueListActivity extends LoadingFragmentPagerActivity {
    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private IssueListFragment mUpdateFragment;
    private IssueListFragment mCommentFragment;
    private IssueListFragment mSubmitFragment;
    private ActionBar mActionBar;
    private Map<String, String> mFilterData;
    private boolean mIsCollaborator;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

    private static final int[] TITLES = new int[] {
        R.string.issues_submitted, R.string.issues_updated, R.string.issues_comments
    };

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mLabels = result.getData();
                showLabelsDialog();
                getSupportLoaderManager().destroyLoader(0);
            }
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback = new LoaderCallbacks<List<Milestone>>() {
        @Override
        public Loader<LoaderResult<List<Milestone>>> onCreateLoader(int id, Bundle args) {
            return new MilestoneListLoader(IssueListActivity.this, mRepoOwner, mRepoName, "open");
        }
        @Override
        public void onResultReady(LoaderResult<List<Milestone>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mMilestones = result.getData();
                showMilestonesDialog();
                getSupportLoaderManager().destroyLoader(1);
            }
        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback =new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new CollaboratorListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mAssignees = result.getData();
                showAssigneesDialog();
                getSupportLoaderManager().destroyLoader(2);
            }
        }
    };

    private LoaderCallbacks<Boolean> mIsCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!checkForError(result)) {
                mIsCollaborator = result.getData();
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);
        mState = data.getString(Constants.Issue.STATE);
        
        mFilterData = new HashMap<String, String>();
        mFilterData.put("state", mState);

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        mActionBar = getSupportActionBar();
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        updateTitle();
    }

    private void updateTitle() {
        if (mState == null || Constants.Issue.STATE_OPEN.equals(mState)) {
            mActionBar.setTitle(R.string.issue_open);
        } else {
            mActionBar.setTitle(R.string.issue_closed);
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        if (position == 1) {
            mUpdateFragment = IssueListByUpdatedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            return mUpdateFragment;
        } else if (position == 2) {
            mCommentFragment = IssueListByCommentsFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            return mCommentFragment;
        } else {
            mSubmitFragment = IssueListBySubmittedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            return mSubmitFragment;
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        if (object instanceof IssueListByUpdatedFragment && mUpdateFragment != object) {
            return true;
        } else if (object instanceof IssueListByCommentsFragment && mUpdateFragment != object) {
            return true;
        } else if (object instanceof IssueListBySubmittedFragment && mSubmitFragment != object) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        menu.findItem(R.id.view_open_closed).setTitle(Constants.Issue.STATE_OPEN.equals(mState)
                ? R.string.issue_view_closed_issues : R.string.issue_view_open_issues);
        if (!mIsCollaborator) {
            menu.removeItem(R.id.view_labels);
            menu.removeItem(R.id.view_milestones);
        }
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openRepositoryInfoActivity(this, mRepoOwner, mRepoName,
                null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.view_open_closed:
            if (Constants.Issue.STATE_OPEN.equals(mState)) {
                mState = Constants.Issue.STATE_CLOSED;
                item.setTitle(R.string.issue_view_open_issues);
            } else {
                mState = Constants.Issue.STATE_OPEN;
                item.setTitle(R.string.issue_view_closed_issues);
            }
            mFilterData.put("state", mState);
            reloadIssueList();
            return true;
        case R.id.create_issue:
            if (Gh4Application.get(this).isAuthorized()) {
                Intent intent = new Intent(this, IssueCreateActivity.class);
                intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.NAME, mRepoName);
                startActivity(intent);
            } else {
                Intent intent = new Intent(this, Github4AndroidActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        case R.id.view_labels:
            Intent intent = new Intent(this, IssueLabelListActivity.class);
            intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.NAME, mRepoName);
            startActivity(intent);
            return true;
        case R.id.view_milestones:
            intent = new Intent(this, IssueMilestoneListActivity.class);
            intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.NAME, mRepoName);
            startActivity(intent);
            return true;
        case R.id.sort:
            String direction = mFilterData.get("direction");
            boolean isDesc = "desc".equals(direction) || direction == null;
            item.setIcon(UiUtils.resolveDrawable(this, isDesc
                    ? R.attr.collapseIcon : R.attr.expandIcon));
            mFilterData.put("direction", isDesc ? "asc" : "desc");
            reloadIssueList();
            return true;
        case R.id.labels:
            if (mLabels == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(0, null, mLabelCallback);
            } else {
                showLabelsDialog();
            }
            return true;
        case R.id.milestones:
            if (mMilestones == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
            } else {
                showMilestonesDialog();
            }
            return true;
        case R.id.assignees:
            if (mAssignees == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
            } else {
                showAssigneesDialog();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadIssueList() {
        updateTitle();
        mSubmitFragment = null;
        mUpdateFragment = null;
        mCommentFragment = null;
        invalidateFragments();
    }
    
    private void showLabelsDialog() {
        String selectedLabels = mFilterData.get("labels");
        String[] checkedLabels = selectedLabels != null ?
                selectedLabels.split(",") : new String[] {};
        List<String> checkLabelStringList = Arrays.asList(checkedLabels);
        final boolean[] checkedItems = new boolean[mLabels.size()];
        final String[] allLabelArray = new String[mLabels.size()];
        
        for (int i = 0; i < mLabels.size(); i++) {
            Label l = mLabels.get(i);
            allLabelArray[i] = l.getName();
            checkedItems[i] = checkLabelStringList.contains(l.getName());
        }
        
        AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_labels);
        builder.setMultiChoiceItems(allLabelArray, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                checkedItems[whichButton] = isChecked;
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String labels = "";
                for (int i = 0; i < allLabelArray.length; i++) {
                    if (checkedItems[i]) {
                        labels += allLabelArray[i] + ",";
                    }
                }
                mFilterData.put("labels", labels);
                reloadIssueList();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    private void showMilestonesDialog() {
        String[] milestones = new String[mMilestones.size() + 1];
        final int[] milestoneIds = new int[mMilestones.size() + 1];
        
        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestoneIds[0] = 0;
        
        String checkedMilestoneNumber = mFilterData.get("milestone");
        int checkedItem = TextUtils.isEmpty(checkedMilestoneNumber)
                ? 0 : Integer.parseInt(checkedMilestoneNumber);
        
        for (int i = 1; i <= mMilestones.size(); i++) {
            Milestone m = mMilestones.get(i - 1);
            milestones[i] = m.getTitle();
            milestoneIds[i] = m.getNumber();
            if (m.getNumber() == checkedItem) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_milestone);
        builder.setSingleChoiceItems(milestones, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mFilterData.remove("milestone");
                } else {
                    mFilterData.put("milestone", String.valueOf(milestoneIds[which]));
                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                reloadIssueList();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    private void showAssigneesDialog() {
        final String[] assignees = new String[mAssignees.size() + 1];
        
        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);
        
        String checkedAssignee = mFilterData.get("assignee");
        int checkedItem = 0;
        
        for (int i = 1; i <= mAssignees.size(); i++) {
            User u = mAssignees.get(i - 1);
            assignees[i] = u.getLogin();
            if (u.getLogin().equalsIgnoreCase(checkedAssignee)) {
                checkedItem = i;
            }
        }
        
        AlertDialog.Builder builder = UiUtils.createDialogBuilder(this);
        builder.setCancelable(true);
        builder.setTitle(R.string.issue_filter_by_assignee);
        builder.setSingleChoiceItems(assignees, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    mFilterData.remove("assignee");
                } else {
                    mFilterData.put("assignee", assignees[which]);
                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                reloadIssueList();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    private boolean checkForError(LoaderResult<?> result) {
        if (result.handleError(IssueListActivity.this)) {
            stopProgressDialog(mProgressDialog);
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }
}