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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueListActivity extends BasePagerActivity {
    private String mRepoOwner;
    private String mRepoName;

    private List<String> mSelectedLabels;
    private int mSelectedMilestone;
    private String mSelectedAssignee;

    private IssueListFragment mOpenFragment;
    private IssueListFragment mClosedFragment;
    private Boolean mIsCollaborator;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

    protected IssueListFragment.SortDrawerAdapter mDrawerAdapter;

    protected static final int ITEM_FILTER_MILESTONE = 1;
    protected static final int ITEM_FILTER_ASSIGNEE = 2;
    protected static final int ITEM_FILTER_LABEL = 3;
    protected static final int ITEM_MANAGE_LABELS = 4;
    protected static final int ITEM_MANAGE_MILESTONES = 5;

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    private static final List<DrawerAdapter.Item> FILTER_DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.SectionHeaderItem(R.string.issue_filter),
        new DrawerAdapter.EntryItem(R.string.issue_filter_by_milestone, 0, ITEM_FILTER_MILESTONE, false),
        new DrawerAdapter.EntryItem(R.string.issue_filter_by_labels, 0, ITEM_FILTER_LABEL, false)
    );

    private static final List<DrawerAdapter.Item> COLLAB_DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.EntryItem(R.string.issue_filter_by_assignee, 0, ITEM_FILTER_ASSIGNEE, false),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.EntryItem(R.string.issue_manage_labels, 0, ITEM_MANAGE_LABELS, false),
        new DrawerAdapter.EntryItem(R.string.issue_manage_milestones, 0, ITEM_MANAGE_MILESTONES, false)
    );

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>() {
        @Override
        public Loader<LoaderResult<List<Label>>> onCreateLoader(int id, Bundle args) {
            return new LabelListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<List<Label>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mLabels = result.getData();
            showLabelsDialog();
            getSupportLoaderManager().destroyLoader(0);
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback =
            new LoaderCallbacks<List<Milestone>>() {
        @Override
        public Loader<LoaderResult<List<Milestone>>> onCreateLoader(int id, Bundle args) {
            return new MilestoneListLoader(IssueListActivity.this, mRepoOwner, mRepoName,
                    Constants.Issue.STATE_OPEN);
        }

        @Override
        public void onResultReady(LoaderResult<List<Milestone>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mMilestones = result.getData();
            showMilestonesDialog();
            getSupportLoaderManager().destroyLoader(1);

        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback =
            new LoaderCallbacks<List<User>>() {
        @Override
        public Loader<LoaderResult<List<User>>> onCreateLoader(int id, Bundle args) {
            return new CollaboratorListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<List<User>> result) {
            if (checkForError(result)) {
                return;
            }
            stopProgressDialog(mProgressDialog);
            mAssignees = result.getData();
            showAssigneesDialog();
            getSupportLoaderManager().destroyLoader(2);
        }
    };

    private LoaderCallbacks<Boolean> mIsCollaboratorCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsCollaboratorLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (checkForError(result)) {
                return;
            }
            if (mIsCollaborator == null) {
                mIsCollaborator = result.getData();
                if (mIsCollaborator) {
                    mDrawerAdapter.addItems(COLLAB_DRAWER_ITEMS);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);

        if (TextUtils.equals(data.getString(Constants.Issue.STATE), Constants.Issue.STATE_CLOSED)) {
            getPager().setCurrentItem(1);
        }

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issues);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected int[][] getTabHeaderColors() {
        return new int[][] {
            {
                UiUtils.resolveColor(this, R.attr.colorIssueOpen),
                UiUtils.resolveColor(this, R.attr.colorIssueOpenDark)
            },
            {
                UiUtils.resolveColor(this, R.attr.colorIssueClosed),
                UiUtils.resolveColor(this, R.attr.colorIssueClosedDark)
            }
        };
    }

    @Override
    protected Fragment getFragment(int position) {
        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", mDrawerAdapter.getSortMode());
        filterData.put("direction", mDrawerAdapter.getSortDirection());
        if (mSelectedLabels != null) {
            filterData.put("labels", TextUtils.join(",", mSelectedLabels));
        }
        if (mSelectedMilestone > 0) {
            filterData.put("milestone", String.valueOf(mSelectedMilestone));
        }
        if (mSelectedAssignee != null) {
            filterData.put("assignee", mSelectedAssignee);
        }

        if (position == 1) {
            filterData.put(Constants.Issue.STATE, Constants.Issue.STATE_CLOSED);
            mClosedFragment = IssueListFragment.newInstance(mRepoOwner, mRepoName, filterData);
            return mClosedFragment;
        } else {
            filterData.put(Constants.Issue.STATE, Constants.Issue.STATE_OPEN);
            mOpenFragment = IssueListFragment.newInstance(mRepoOwner, mRepoName, filterData);
            return mOpenFragment;
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        if (object instanceof IssueListFragment) {
            if (object != mOpenFragment && object != mClosedFragment) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ListAdapter getRightNavigationDrawerAdapter() {
        mDrawerAdapter = IssueListFragment.SortDrawerAdapter.create(this);
        mDrawerAdapter.addItems(FILTER_DRAWER_ITEMS);
        if (mIsCollaborator != null && mIsCollaborator) {
            mDrawerAdapter.addItems(COLLAB_DRAWER_ITEMS);
        }
        return mDrawerAdapter;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, R.string.actions)
                .setIcon(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            toggleRightSideDrawer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }

    @Override
    protected boolean onDrawerItemSelected(boolean left, int position) {
        if (mDrawerAdapter.handleSortModeChange(position)) {
            reloadIssueList();
            return true;
        }

        if (left) {
            return super.onDrawerItemSelected(left, position);
        }

        int id = (int) mDrawerAdapter.getItemId(position);
        switch (id) {
            case ITEM_FILTER_ASSIGNEE:
                filterAssignee();
                return true;
            case ITEM_FILTER_LABEL:
                filterLabel();
                return true;
            case ITEM_FILTER_MILESTONE:
                filterMilestone();
                return true;
            case ITEM_MANAGE_LABELS:
                manageLabels();
                return true;
            case ITEM_MANAGE_MILESTONES:
                manageMilestones();
                return true;
        }
        return false;
    }

    private void reloadIssueList() {
        mOpenFragment = null;
        mClosedFragment = null;
        invalidateFragments();
    }

    private void showLabelsDialog() {
        final boolean[] checkedItems = new boolean[mLabels.size()];
        final String[] allLabelArray = new String[mLabels.size()];

        for (int i = 0; i < mLabels.size(); i++) {
            Label l = mLabels.get(i);
            allLabelArray[i] = l.getName();
            checkedItems[i] = mSelectedLabels != null && mSelectedLabels.contains(l.getName());
        }

        DialogInterface.OnMultiChoiceClickListener selectCb =
                new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton, boolean isChecked) {
                checkedItems[whichButton] = isChecked;
            }
        };
        DialogInterface.OnClickListener okCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                mSelectedLabels = new ArrayList<>();
                for (int i = 0; i < allLabelArray.length; i++) {
                    if (checkedItems[i]) {
                        mSelectedLabels.add(allLabelArray[i]);
                    }
                }
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_labels)
                .setMultiChoiceItems(allLabelArray, checkedItems, selectCb)
                .setPositiveButton(R.string.ok, okCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMilestonesDialog() {
        String[] milestones = new String[mMilestones.size() + 1];
        final int[] milestoneIds = new int[mMilestones.size() + 1];
        int selected = 0;

        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestoneIds[0] = 0;

        for (int i = 1; i <= mMilestones.size(); i++) {
            Milestone m = mMilestones.get(i - 1);
            milestones[i] = m.getTitle();
            milestoneIds[i] = m.getNumber();
            if (m.getNumber() == mSelectedMilestone) {
                selected = i;
            }
        }

        DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedMilestone = milestoneIds[which];
                dialog.dismiss();
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_milestone)
                .setSingleChoiceItems(milestones, selected, selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showAssigneesDialog() {
        final String[] assignees = new String[mAssignees.size() + 1];
        int selected = 0;

        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);

        for (int i = 1; i <= mAssignees.size(); i++) {
            User u = mAssignees.get(i - 1);
            assignees[i] = u.getLogin();
            if (u.getLogin().equalsIgnoreCase(mSelectedAssignee)) {
                selected = i;
            }
        }

        DialogInterface.OnClickListener selectCb = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedAssignee = which != 0 ? mAssignees.get(which - 1).getLogin() : null;
                dialog.dismiss();
                reloadIssueList();
            }
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_assignee)
                .setSingleChoiceItems(assignees, selected, selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean checkForError(LoaderResult<?> result) {
        if (result.handleError(IssueListActivity.this)) {
            stopProgressDialog(mProgressDialog);
            supportInvalidateOptionsMenu();
            return true;
        }
        return false;
    }

    private void filterAssignee() {
        if (mAssignees == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(2, null, mCollaboratorListCallback);
        } else {
            showAssigneesDialog();
        }
    }

    private void filterMilestone() {
        if (mMilestones == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(1, null, mMilestoneCallback);
        } else {
            showMilestonesDialog();
        }
    }

    private void filterLabel() {
        if (mLabels == null) {
            mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
            getSupportLoaderManager().initLoader(0, null, mLabelCallback);
        } else {
            showLabelsDialog();
        }
    }

    private void manageLabels() {
        Intent intent = new Intent(this, IssueLabelListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        startActivity(intent);
    }

    private void manageMilestones() {
        Intent intent = new Intent(this, IssueMilestoneListActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        startActivity(intent);
    }
}
