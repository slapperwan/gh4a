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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class IssueListActivity extends BasePagerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener,
        SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {
    private static final int REQUEST_ISSUE_CREATE = 1001;

    private String mRepoOwner;
    private String mRepoName;

    private List<String> mSelectedLabels;
    private int mSelectedMilestone;
    private String mSelectedAssignee;
    private String mSearchQuery;
    private boolean mSearchMode;

    private FloatingActionButton mCreateFab;
    private IssueListFragment mOpenFragment;
    private IssueListFragment mClosedFragment;
    private IssueListFragment mSearchFragment;
    private Boolean mIsCollaborator;
    private ProgressDialog mProgressDialog;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;

    protected IssueListFragment.SortDrawerHelper mSortHelper =
            new IssueListFragment.SortDrawerHelper();

    private static final String STATE_KEY_SEARCH_QUERY = "search_query";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";

    private static final String LIST_QUERY = "is:issue is:%s repo:%s/%s %s %s %s";
    private static final String SEARCH_QUERY = "is:%s repo:%s/%s %s";

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    private LoaderCallbacks<List<Label>> mLabelCallback = new LoaderCallbacks<List<Label>>(this) {
        @Override
        protected Loader<LoaderResult<List<Label>>> onCreateLoader() {
            return new LabelListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(List<Label> result) {
            stopProgressDialog(mProgressDialog);
            mLabels = result;
            showLabelsDialog();
            getSupportLoaderManager().destroyLoader(0);
        }

        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private LoaderCallbacks<List<Milestone>> mMilestoneCallback =
            new LoaderCallbacks<List<Milestone>>(this) {
        @Override
        protected Loader<LoaderResult<List<Milestone>>> onCreateLoader() {
            return new MilestoneListLoader(IssueListActivity.this, mRepoOwner, mRepoName,
                    Constants.Issue.STATE_OPEN);
        }

        @Override
        protected void onResultReady(List<Milestone> result) {
            stopProgressDialog(mProgressDialog);
            mMilestones = result;
            showMilestonesDialog();
            getSupportLoaderManager().destroyLoader(1);
        }

        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private LoaderCallbacks<List<User>> mCollaboratorListCallback =
            new LoaderCallbacks<List<User>>(this) {
        @Override
        protected Loader<LoaderResult<List<User>>> onCreateLoader() {
            return new CollaboratorListLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(List<User> result) {
            stopProgressDialog(mProgressDialog);
            mAssignees = result;
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
            return new IsCollaboratorLoader(IssueListActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(Boolean result) {
            if (mIsCollaborator == null) {
                mIsCollaborator = result;
                if (mIsCollaborator) {
                    updateRightNavigationDrawer();
                }
            }
        }

        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String state = getIntent().getStringExtra(Constants.Issue.STATE);
        if (TextUtils.equals(state, Constants.Issue.STATE_CLOSED)) {
            getPager().setCurrentItem(1);
        }

        if (savedInstanceState != null) {
            mSearchQuery = savedInstanceState.getString(STATE_KEY_SEARCH_QUERY);
            mSearchMode = savedInstanceState.getBoolean(STATE_KEY_SEARCH_MODE);
        }

        if (Gh4Application.get().isAuthorized()) {
            CoordinatorLayout rootLayout = getRootLayout();
            mCreateFab = (FloatingActionButton) getLayoutInflater().inflate(
                    R.layout.add_fab, rootLayout, false);
            mCreateFab.setOnClickListener(this);
            rootLayout.addView(mCreateFab);
        }

        getSupportLoaderManager().initLoader(3, null, mIsCollaboratorCallback);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.issues);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
    }

    @Override
    public void onRefresh() {
        mAssignees = null;
        mMilestones = null;
        mLabels = null;
        mIsCollaborator = null;
        updateRightNavigationDrawer();

        LoaderManager lm = getSupportLoaderManager();
        for (int i = 0; i < 4; i++) {
            Loader loader = lm.getLoader(i);
            if (loader != null) {
                loader.onContentChanged();
            }
        }
        super.onRefresh();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        outState.putString(STATE_KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(STATE_KEY_SEARCH_MODE, mSearchMode);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    public void onRecyclerViewCreated(Fragment fragment, RecyclerView recyclerView) {
        if (fragment == mOpenFragment) {
            recyclerView.setTag(R.id.FloatingActionButtonScrollEnabled, new Object());
        }
    }

    @Override
    protected void onPageMoved(int position, float fraction) {
        super.onPageMoved(position, fraction);
        if (!mSearchMode && mCreateFab != null) {
            float openFraction = 1 - position - fraction;
            ViewCompat.setScaleX(mCreateFab, openFraction);
            ViewCompat.setScaleY(mCreateFab, openFraction);
            mCreateFab.setVisibility(openFraction == 0 ? View.INVISIBLE : View.VISIBLE);
        }
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
        if (mSearchMode) {
            return makeSearchFragment(position);
        } else {
            return makeListFragment(position);
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        if (object instanceof IssueListFragment) {
            if (mSearchMode && object != mSearchFragment) {
                return true;
            } else if (!mSearchMode && object != mOpenFragment && object != mClosedFragment) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ISSUE_CREATE) {
            if (resultCode == Activity.RESULT_OK && mOpenFragment != null) {
                mOpenFragment.onRefresh();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected int[] getRightNavigationDrawerMenuResources() {
        int[] menuResIds = new int[mSearchMode ? 1 : 2];
        menuResIds[0] = IssueListFragment.SortDrawerHelper.getMenuResId();
        if (!mSearchMode) {
            menuResIds[1] = mIsCollaborator != null && mIsCollaborator
                    ? R.menu.issue_list_filter_collab : R.menu.issue_list_filter;
        }
        return menuResIds;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        super.onNavigationItemSelected(item);

        if (mSortHelper.handleItemSelection(item)) {
            reloadIssueList();
            return true;
        }

        switch (item.getItemId()) {
            case R.id.filter_by_assignee:
                filterAssignee();
                return true;
            case R.id.filter_by_label:
                filterLabel();
                return true;
            case R.id.filter_by_milestone:
                filterMilestone();
                return true;
            case R.id.manage_labels:
                manageLabels();
                return true;
            case R.id.manage_milestones:
                manageMilestones();
                return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.issue_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        MenuItemCompat.setOnActionExpandListener(searchItem, this);

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (mSearchQuery != null) {
            MenuItemCompat.expandActionView(searchItem);
            searchView.setQuery(mSearchQuery, false);
        }
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.overflow) {
            toggleRightSideDrawer();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mSearchQuery = null;
        setSearchMode(false);
        return true;
    }

    @Override
    public boolean onClose() {
        mSearchQuery = null;
        setSearchMode(false);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        setSearchMode(true);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchQuery = newText;
        return false;
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent(this, IssueEditActivity.class);
        intent.putExtra(Constants.Repository.OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.NAME, mRepoName);
        startActivityForResult(intent, REQUEST_ISSUE_CREATE);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }

    private void setSearchMode(boolean enabled) {
        boolean changed = mSearchMode != enabled;
        mSearchMode = enabled;
        mCreateFab.setVisibility(enabled ? View.GONE : View.VISIBLE);
        reloadIssueList();
        if (changed) {
            updateRightNavigationDrawer();
        }
    }

    private Fragment makeListFragment(int position) {
        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", mSortHelper.getSortMode());
        filterData.put("direction", mSortHelper.getSortDirection());
        filterData.put("q", String.format(Locale.US, LIST_QUERY,
                position == 1 ? Constants.Issue.STATE_CLOSED : Constants.Issue.STATE_OPEN,
                mRepoOwner, mRepoName,
                mSelectedLabels != null ? "labels:" + TextUtils.join(",", mSelectedLabels) : "",
                mSelectedMilestone > 0 ? "milestone:" + mSelectedMilestone : "", // XXX: milestone name?
                mSelectedAssignee != null ? "assignee:" + mSelectedAssignee : ""));

        final IssueListFragment f = IssueListFragment.newInstance(filterData,
                position == 1, R.string.no_issues_found, false);

        if (position == 1) {
            mClosedFragment = f;
        } else {
            mOpenFragment = f;
        }
        return f;
    }

    private Fragment makeSearchFragment(int position) {
        boolean closed = position == 1;
        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", mSortHelper.getSortMode());
        filterData.put("direction", mSortHelper.getSortDirection());
        filterData.put("q", String.format(Locale.US, SEARCH_QUERY,
                closed ? Constants.Issue.STATE_CLOSED : Constants.Issue.STATE_OPEN,
                mRepoOwner, mRepoName, mSearchQuery));

        mSearchFragment = IssueListFragment.newInstance(filterData, closed,
                R.string.no_search_issues_found, false);
        return mSearchFragment;
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
