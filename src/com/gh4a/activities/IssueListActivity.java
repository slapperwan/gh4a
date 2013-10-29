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
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
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
import com.gh4a.utils.UiUtils;

public class IssueListActivity extends BaseSherlockFragmentActivity implements OnClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private ThisPageAdapter mAdapter;
    private IssueListFragment mUpdateFragment;
    private IssueListFragment mCommentFragment;
    private IssueListFragment mSubmitFragment;
    private ActionBar mActionBar;
    private Map<String, String> mFilterData;
    private boolean isCollaborator;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

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
                isCollaborator = result.getData();
                invalidateOptionsMenu();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mState = data.getString(Constants.Issue.ISSUE_STATE);
        
        mFilterData = new HashMap<String, String>();
        mFilterData.put("state", mState);

        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.view_pager);

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        mActionBar = getSupportActionBar();
        updateTitle();
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        setupPager(mAdapter, new int[] {
            R.string.issues_submitted, R.string.issues_updated, R.string.issues_comments
        });
    }

    private void updateTitle() {
        if (mState == null || "open".equals(mState)) {
            mActionBar.setTitle(R.string.issue_open);
        }
        else {
            mActionBar.setTitle(R.string.issue_closed);
        }
    }
    
    public class ThisPageAdapter extends FragmentStatePagerAdapter {

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 1) {
                mUpdateFragment = IssueListByUpdatedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
                return mUpdateFragment;
            }
            else if (position == 2) {
                mCommentFragment = IssueListByCommentsFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
                return mCommentFragment;
            }
            else {
                mSubmitFragment = IssueListBySubmittedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
                return mSubmitFragment;
            }
        }
        
        @Override
        public int getItemPosition(Object object) {
            if (object instanceof IssueListByUpdatedFragment && mUpdateFragment != object) {
                return POSITION_NONE;
            }
            else if (object instanceof IssueListByCommentsFragment && mUpdateFragment != object) {
                return POSITION_NONE;
            }
            else if (object instanceof IssueListBySubmittedFragment && mSubmitFragment != object) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        if ("open".equals(mState)) {
            menu.getItem(4).setTitle(R.string.issue_view_closed_issues);
        }
        else {
            menu.getItem(4).setTitle(R.string.issue_view_open_issues);
        }
        if (!isCollaborator) {
            menu.removeItem(R.id.view_labels);
            menu.removeItem(R.id.view_milestones);
        }
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.navigation_expand_dark);
            menu.getItem(1).setIcon(R.drawable.collections_labels_dark);
            menu.getItem(2).setIcon(R.drawable.collections_view_as_list_dark);
            menu.getItem(3).setIcon(R.drawable.social_person_dark);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openRepositoryInfoActivity(this,
                mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.view_open_closed:
            if ("open".equals(mState)) {
                mState = Constants.Issue.ISSUE_STATE_CLOSED;
                mFilterData.put("state", Constants.Issue.ISSUE_STATE_CLOSED);
                item.setTitle(R.string.issue_view_open_issues);
            }
            else {
                mState = Constants.Issue.ISSUE_STATE_OPEN;
                mFilterData.put("state", Constants.Issue.ISSUE_STATE_OPEN);
                item.setTitle(R.string.issue_view_closed_issues);
            }
            reloadIssueList();
            return true;
        case R.id.create_issue:
            if (Gh4Application.get(this).isAuthorized()) {
                Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
            }
            else {
                Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                startActivity(intent);
                finish();
            }
            return true;
        case R.id.view_labels:
            Intent intent = new Intent().setClass(IssueListActivity.this, IssueLabelListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
            return true;
        case R.id.view_milestones:
            intent = new Intent().setClass(IssueListActivity.this, IssueMilestoneListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            startActivity(intent);
            return true;
        case R.id.sort:
            String direction = mFilterData.get("direction");
            if ("desc".equals(direction) || direction == null) {
                if (Gh4Application.THEME == R.style.LightTheme) {
                    item.setIcon(R.drawable.navigation_collapse);
                }
                else {
                    item.setIcon(R.drawable.navigation_collapse_dark);
                }
                mFilterData.put("direction", "asc");
            }
            else {
                if (Gh4Application.THEME == R.style.LightTheme) {
                    item.setIcon(R.drawable.navigation_expand);
                }
                else {
                    item.setIcon(R.drawable.navigation_expand_dark);
                }
                mFilterData.put("direction", "desc");
            }
            reloadIssueList();
            return true;
        case R.id.labels:
            if (mLabels == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(0, null, mLabelCallback);
            }
            else {
                showLabelsDialog();
            }
            return true;
        case R.id.milestones:
            if (mMilestones == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
            }
            else {
                showMilestonesDialog();
            }
            return true;
        case R.id.assignees:
            if (mAssignees == null) {
                mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
            }
            else {
                showAssigneesDialog();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        default:
            break;
        }
    }
    
    private void reloadIssueList() {
        updateTitle();
        mSubmitFragment = null;
        mUpdateFragment = null;
        mCommentFragment = null;
        mAdapter.notifyDataSetChanged();
    }
    
    private void showLabelsDialog() {
        String selectedLabels = mFilterData.get("labels");
        String[] checkedLabels = new String[] {};
        
        if (selectedLabels != null) {
            checkedLabels = selectedLabels.split(",");
        }
        List<String> checkLabelStringList = Arrays.asList(checkedLabels);
        final boolean[] checkedItems = new boolean[mLabels.size()];

        final String[] allLabelArray = new String[mLabels.size()];
        
        for (int i = 0; i < mLabels.size(); i++) {
            Label l = mLabels.get(i);
            allLabelArray[i] = l.getName();
            if(checkLabelStringList.contains(l.getName())) {
                checkedItems[i] = true;
            }
            else {
                checkedItems[i] = false;
            }
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
        int checkedItem = checkedMilestoneNumber != null && !"".equals(checkedMilestoneNumber) ? 
                 Integer.parseInt(checkedMilestoneNumber) : 0;
        
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
                }
                else {
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
                }
                else {
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
