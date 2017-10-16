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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.fragment.LoadingListFragmentBase;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Issue;
import com.meisolsson.githubsdk.model.IssueState;
import com.meisolsson.githubsdk.model.Label;
import com.meisolsson.githubsdk.model.Milestone;
import com.meisolsson.githubsdk.model.User;
import com.meisolsson.githubsdk.service.issues.IssueAssigneeService;
import com.meisolsson.githubsdk.service.issues.IssueLabelService;
import com.meisolsson.githubsdk.service.issues.IssueMilestoneService;
import com.meisolsson.githubsdk.service.repositories.RepositoryCollaboratorService;
import com.philosophicalhacker.lib.RxLoader;

import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;

public class IssueListActivity extends BaseFragmentPagerActivity implements
        View.OnClickListener, LoadingListFragmentBase.OnRecyclerViewCreatedListener,
        SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener {
    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return makeIntent(context, repoOwner, repoName, false);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
            boolean isPullRequest) {
        return new Intent(context, IssueListActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("is_pull_request", isPullRequest);
    }

    private static final int REQUEST_ISSUE_CREATE = 1001;
    private static final int REQUEST_ISSUE_AFTER_CREATE = 1002;

    private static final int ID_LOADER_COLLABORATOR_STATUS = 0;

    private String mRepoOwner;
    private String mRepoName;
    private String mUserLogin;
    private boolean mIsPullRequest;

    private String mSelectedLabel;
    private String mSelectedMilestone;
    private String mSelectedAssignee;
    private String mSearchQuery;
    private boolean mSearchMode;
    private boolean mSearchIsExpanded;
    private int mSelectedParticipatingStatus = 0;

    private FloatingActionButton mCreateFab;
    private IssueListFragment mOpenFragment;
    private Boolean mIsCollaborator;
    private List<Label> mLabels;
    private List<Milestone> mMilestones;
    private List<User> mAssignees;
    private RxLoader mRxLoader;

    private final IssueListFragment.SortDrawerHelper mSortHelper =
            new IssueListFragment.SortDrawerHelper();

    private static final String STATE_KEY_SEARCH_QUERY = "search_query";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";
    private static final String STATE_KEY_SEARCH_IS_EXPANDED = "search_is_expanded";
    private static final String STATE_KEY_SELECTED_MILESTONE = "selected_milestone";
    private static final String STATE_KEY_SELECTED_LABEL = "selected_label";
    private static final String STATE_KEY_SELECTED_ASSIGNEE = "selected_assignee";
    private static final String STATE_KEY_PARTICIPATING_STATUS = "participating_status";

    private static final String LIST_QUERY = "is:%s %s repo:%s/%s %s %s %s %s";
    private static final String SEARCH_QUERY = "is:%s %s repo:%s/%s %s";

    private static final int[] TITLES = new int[] {
        R.string.open, R.string.closed
    };

    private static final int[] PULL_REQUEST_TITLES = new int[] {
        R.string.open, R.string.closed, R.string.merged
    };

    private static final int[][] HEADER_COLOR_ATTRS = new int[][] {
        { R.attr.colorIssueOpen, R.attr.colorIssueOpenDark },
        { R.attr.colorIssueClosed, R.attr.colorIssueClosedDark },
        { R.attr.colorPullRequestMerged, R.attr.colorPullRequestMergedDark }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUserLogin = Gh4Application.get().getAuthLogin();

        if (savedInstanceState != null) {
            mSearchQuery = savedInstanceState.getString(STATE_KEY_SEARCH_QUERY);
            mSearchMode = savedInstanceState.getBoolean(STATE_KEY_SEARCH_MODE);
            mSearchIsExpanded = savedInstanceState.getBoolean(STATE_KEY_SEARCH_IS_EXPANDED);
            mSelectedLabel = savedInstanceState.getString(STATE_KEY_SELECTED_LABEL);
            mSelectedAssignee = savedInstanceState.getString(STATE_KEY_SELECTED_ASSIGNEE);
            mSelectedParticipatingStatus =
                    savedInstanceState.getInt(STATE_KEY_PARTICIPATING_STATUS);
        }

        if (!mIsPullRequest && Gh4Application.get().isAuthorized()) {
            CoordinatorLayout rootLayout = getRootLayout();
            mCreateFab = (FloatingActionButton) getLayoutInflater().inflate(
                    R.layout.add_fab, rootLayout, false);
            mCreateFab.setOnClickListener(this);
            rootLayout.addView(mCreateFab);
        }

        mRxLoader = new RxLoader(this, getSupportLoaderManager());
        loadCollaboratorStatus(false);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(mIsPullRequest ? R.string.pull_requests : R.string.issues);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mIsPullRequest = extras.getBoolean("is_pull_request");
    }

    @Override
    public void onRefresh() {
        mAssignees = null;
        mMilestones = null;
        mLabels = null;
        mIsCollaborator = null;
        updateRightNavigationDrawer();
        loadCollaboratorStatus(true);
        super.onRefresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_KEY_SEARCH_QUERY, mSearchQuery);
        outState.putBoolean(STATE_KEY_SEARCH_MODE, mSearchMode);
        outState.putBoolean(STATE_KEY_SEARCH_IS_EXPANDED, mSearchIsExpanded);
        outState.putString(STATE_KEY_SELECTED_MILESTONE, mSelectedMilestone);
        outState.putString(STATE_KEY_SELECTED_LABEL, mSelectedLabel);
        outState.putString(STATE_KEY_SELECTED_ASSIGNEE, mSelectedAssignee);
        outState.putInt(STATE_KEY_PARTICIPATING_STATUS, mSelectedParticipatingStatus);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mIsPullRequest ? PULL_REQUEST_TITLES : TITLES;
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
            mCreateFab.setScaleX(openFraction);
            mCreateFab.setScaleY(openFraction);
            mCreateFab.setVisibility(openFraction == 0 ? View.INVISIBLE : View.VISIBLE);
        }
    }

    @Override
    protected int[][] getTabHeaderColorAttrs() {
        return HEADER_COLOR_ATTRS;
    }

    @Override
    protected Fragment makeFragment(int position) {
        final @StringRes int emptyTextResId;
        final String query;

        if (mSearchMode) {
            query = String.format(Locale.US, SEARCH_QUERY,
                    mIsPullRequest ? "pr" : "issue",
                    getIssueType(position), mRepoOwner, mRepoName, mSearchQuery);
            emptyTextResId = mIsPullRequest
                    ? R.string.no_search_pull_requests_found : R.string.no_search_issues_found;
        } else {
            query = String.format(Locale.US, LIST_QUERY,
                    mIsPullRequest ? "pr" : "issue",
                    getIssueType(position), mRepoOwner, mRepoName,
                    buildFilterItem("assignee", mSelectedAssignee),
                    buildFilterItem("label", mSelectedLabel),
                    buildFilterItem("milestone", mSelectedMilestone),
                    buildParticipatingFilterItem()).trim();
            emptyTextResId = mIsPullRequest
                    ? R.string.no_pull_requests_found : R.string.no_issues_found;
        }

        return IssueListFragment.newInstance(query,
                mSortHelper.getSortMode(), mSortHelper.getSortOrder(),
                getIssueState(position), emptyTextResId, false);
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        if (position == 0) {
            mOpenFragment = (IssueListFragment) f;
        }
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mOpenFragment) {
            mOpenFragment = null;
        }
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ISSUE_CREATE) {
            if (resultCode == Activity.RESULT_OK && data.hasExtra("issue")) {
                Issue issue = data.getParcelableExtra("issue");
                Intent intent = IssueActivity.makeIntent(this, mRepoOwner, mRepoName,
                        issue.number());
                startActivityForResult(intent, REQUEST_ISSUE_AFTER_CREATE);
            }
        } else if (requestCode == REQUEST_ISSUE_AFTER_CREATE) {
            // Refresh all fragments (instead of just open issues fragment) when coming back from
            // newly created issue as there is a chance that it could be immediately closed
            super.onRefresh();
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
    protected int getInitialRightDrawerSelection() {
        return R.id.sort_created_desc;
    }

    @Override
    protected void onPrepareRightNavigationDrawerMenu(Menu menu) {
        super.onPrepareRightNavigationDrawerMenu(menu);
        MenuItem milestoneFilterItem = menu.findItem(R.id.filter_by_milestone);
        if (milestoneFilterItem != null) {
            final String subtitle =
                    mSelectedMilestone == null ? getString(R.string.issue_filter_by_any_milestone) :
                    mSelectedMilestone.isEmpty() ? getString(R.string.issue_filter_by_no_milestone) :
                    mSelectedMilestone;
            UiUtils.setMenuItemText(this, milestoneFilterItem,
                    getString(R.string.issue_filter_by_milestone), subtitle);
        }
        MenuItem labelFilterItem = menu.findItem(R.id.filter_by_label);
        if (labelFilterItem != null) {
            final String subtitle =
                    mSelectedLabel == null ? getString(R.string.issue_filter_by_any_label) :
                    mSelectedLabel.isEmpty() ? getString(R.string.issue_filter_by_no_label) :
                    mSelectedLabel;
            UiUtils.setMenuItemText(this, labelFilterItem,
                    getString(R.string.issue_filter_by_labels), subtitle);
        }
        MenuItem assigneeFilterItem = menu.findItem(R.id.filter_by_assignee);
        if (assigneeFilterItem != null) {
            final String subtitle =
                    mSelectedAssignee == null ? getString(R.string.issue_filter_by_any_assignee) :
                    mSelectedAssignee.isEmpty() ? getString(R.string.issue_filter_by_no_assignee) :
                    mSelectedAssignee;
            UiUtils.setMenuItemText(this, assigneeFilterItem,
                    getString(R.string.issue_filter_by_assignee), subtitle);
        }
        MenuItem participatingFilterItem = menu.findItem(R.id.filter_by_participating);
        if (participatingFilterItem != null) {
            String[] valueStrings = getResources().getStringArray(R.array.filter_participating);
            UiUtils.setMenuItemText(this, participatingFilterItem,
                    getString(R.string.issue_filter_by_participating),
                    valueStrings[mSelectedParticipatingStatus]);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        super.onNavigationItemSelected(item);

        if (mSortHelper.handleItemSelection(item)) {
            invalidateFragments();
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
            case R.id.filter_by_participating:
                filterParticipating();
                return true;
            case R.id.manage_labels:
                startActivity(IssueLabelListActivity.makeIntent(this,
                        mRepoOwner, mRepoName, mIsPullRequest));
                return true;
            case R.id.manage_milestones:
                startActivity(IssueMilestoneListActivity.makeIntent(this,
                        mRepoOwner, mRepoName, mIsPullRequest));
                return true;
        }

        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.issue_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchItem.setOnActionExpandListener(this);

        final SearchView searchView = (SearchView) searchItem.getActionView();
        if (mSearchIsExpanded) {
            searchItem.expandActionView();
            searchView.setQuery(mSearchQuery, false);
        }
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

        if (mSelectedMilestone != null || mSelectedAssignee != null
                || mSelectedLabel != null || mSelectedParticipatingStatus != 0) {
            menu.findItem(R.id.remove_filter).setVisible(true);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.overflow) {
            toggleRightSideDrawer();
            return true;
        }
        if (item.getItemId() == R.id.remove_filter) {
            removeFilter();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeFilter() {
        mSelectedMilestone = null;
        mSelectedAssignee = null;
        mSelectedLabel = null;
        mSelectedParticipatingStatus = 0;
        onFilterUpdated();
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mSearchIsExpanded = true;
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mSearchIsExpanded = false;
        mSearchQuery = null;
        setSearchMode(false);
        return true;
    }

    @Override
    public boolean onClose() {
        mSearchIsExpanded = false;
        mSearchQuery = null;
        setSearchMode(false);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchQuery = query;
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
        Intent intent = IssueEditActivity.makeCreateIntent(this, mRepoOwner, mRepoName);
        startActivityForResult(intent, REQUEST_ISSUE_CREATE);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName);
    }

    private void setSearchMode(boolean enabled) {
        boolean changed = mSearchMode != enabled;
        mSearchMode = enabled;
        if (mCreateFab != null) {
            mCreateFab.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
        invalidateFragments();
        if (changed) {
            updateRightNavigationDrawer();
        }
    }

    private String getIssueState(int position) {
        switch (position) {
            case 1:
                return ApiHelpers.IssueState.CLOSED;
            case 2:
                return ApiHelpers.IssueState.MERGED;
            default:
                return ApiHelpers.IssueState.OPEN;
        }
    }

    private String getIssueType(int position) {
        String type = "is:" + getIssueState(position);
        if (position == 1 && mIsPullRequest) {
            type += " is:" + ApiHelpers.IssueState.UNMERGED;
        }
        return type;
    }

    private String buildParticipatingFilterItem() {
        if (mSelectedParticipatingStatus == 1) {
            return "involves:" + mUserLogin;
        } else if (mSelectedParticipatingStatus == 2) {
            return "-involves:" + mUserLogin;
        } else {
            return "";
        }
    }

    private String buildFilterItem(String type, String value) {
        if (!TextUtils.isEmpty(value)) {
            return type + ":\"" + value + "\"";
        } else if (value == null) {
            // null means 'any value'
            return "";
        } else {
            // empty string means 'no value set
            return "no:" + type;
        }
    }

    private void showLabelsDialog() {
        final String[] labels = new String[mLabels.size() + 2];
        int selected = mSelectedLabel != null && mSelectedLabel.isEmpty() ? 1 : 0;

        labels[0] = getResources().getString(R.string.issue_filter_by_any_label);
        labels[1] = getResources().getString(R.string.issue_filter_by_no_label);

        for (int i = 0; i < mLabels.size(); i++) {
            labels[i + 2] = mLabels.get(i).name();
            if (TextUtils.equals(mSelectedLabel, labels[i + 2])) {
                selected = i + 2;
            }
        }

        DialogInterface.OnClickListener selectCb = (dialog, which) -> {
            mSelectedLabel = which == 0 ? null : which == 1 ? "" : labels[which];
            dialog.dismiss();
            onFilterUpdated();
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_labels)
                .setSingleChoiceItems(labels, selected, selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showMilestonesDialog() {
        final String[] milestones = new String[mMilestones.size() + 2];
        int selected = mSelectedMilestone != null && mSelectedMilestone.isEmpty() ? 1 : 0;

        milestones[0] = getResources().getString(R.string.issue_filter_by_any_milestone);
        milestones[1] = getResources().getString(R.string.issue_filter_by_no_milestone);

        for (int i = 0; i < mMilestones.size(); i++) {
            milestones[i + 2] = mMilestones.get(i).title();
            if (TextUtils.equals(mSelectedMilestone, milestones[i + 2])) {
                selected = i + 2;
            }
        }

        DialogInterface.OnClickListener selectCb = (dialog, which) -> {
            mSelectedMilestone =
                    which == 0 ? null : which == 1 ? "" : milestones[which];
            dialog.dismiss();
            onFilterUpdated();
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_milestone)
                .setSingleChoiceItems(milestones, selected, selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void onFilterUpdated() {
        invalidateFragments();
        invalidateOptionsMenu();
        updateRightNavigationDrawer();
    }

    private void showAssigneesDialog() {
        final String[] assignees = new String[mAssignees.size() + 2];
        int selected = mSelectedAssignee != null && mSelectedAssignee.isEmpty() ? 1 : 0;

        assignees[0] = getResources().getString(R.string.issue_filter_by_any_assignee);
        assignees[1] = getResources().getString(R.string.issue_filter_by_no_assignee);

        for (int i = 0; i < mAssignees.size(); i++) {
            User u = mAssignees.get(i);
            assignees[i + 2] = u.login();
            if (u.login().equalsIgnoreCase(mSelectedAssignee)) {
                selected = i + 2;
            }
        }

        DialogInterface.OnClickListener selectCb = (dialog, which) -> {
            mSelectedAssignee =
                    which == 0 ? null : which == 1 ? "" : mAssignees.get(which - 2).login();
            dialog.dismiss();
            onFilterUpdated();
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_assignee)
                .setSingleChoiceItems(assignees, selected, selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void filterAssignee() {
        if (mAssignees != null) {
            showAssigneesDialog();
        } else {
            final IssueAssigneeService service =
                    Gh4Application.get().getGitHubService(IssueAssigneeService.class);
            registerTemporarySubscription(ApiHelpers.PageIterator
                    .toSingle(page -> service.getAssignees(mRepoOwner, mRepoName, page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(getBaseActivity(), R.string.loading_msg))
                    .subscribe(assignees -> {
                        mAssignees = assignees;
                        showAssigneesDialog();
                    }, error -> {}));
        }
    }

    private void filterMilestone() {
        if (mMilestones != null) {
            showMilestonesDialog();
        } else {
            final IssueMilestoneService service =
                    Gh4Application.get().getGitHubService(IssueMilestoneService.class);
            registerTemporarySubscription(ApiHelpers.PageIterator
                    .toSingle(page -> service.getRepositoryMilestones(mRepoOwner, mRepoName, "open", page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(getBaseActivity(), R.string.loading_msg))
                    .subscribe(milestones -> {
                        mMilestones = milestones;
                        showMilestonesDialog();
                    }, error -> {}));
        }
    }

    private void filterLabel() {
        if (mLabels != null) {
            showLabelsDialog();
        } else {
            final IssueLabelService service =
                    Gh4Application.get().getGitHubService(IssueLabelService.class);
            registerTemporarySubscription(ApiHelpers.PageIterator
                    .toSingle(page -> service.getRepositoryLabels(mRepoOwner, mRepoName, page))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(getBaseActivity(), R.string.loading_msg))
                    .subscribe(labels -> {
                        mLabels = labels;
                        showLabelsDialog();
                    }, error -> {}));
        }
    }

    private void filterParticipating() {
        DialogInterface.OnClickListener selectCb = (dialog, which) -> {
            mSelectedParticipatingStatus = which;
            dialog.dismiss();
            onFilterUpdated();
        };

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.issue_filter_by_participating)
                .setSingleChoiceItems(R.array.filter_participating, mSelectedParticipatingStatus,
                        selectCb)
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void loadCollaboratorStatus(boolean force) {
        Gh4Application app = Gh4Application.get();
        String login = app.getAuthLogin();
        final Observable<Boolean> observable;

        if (login == null) {
            observable = Observable.just(false);
        } else {
            RepositoryCollaboratorService service =
                    app.getGitHubService(RepositoryCollaboratorService.class);
            // TODO: consider moving to a shared place
            observable = service.isUserCollaborator(mRepoOwner, mRepoName, login)
                    .map(ApiHelpers::throwOnFailure)
                    .compose(RxUtils::doInBackground)
                    // the API returns 403 if the user doesn't have push access,
                    // which in turn means he isn't a collaborator
                    .compose(RxUtils.mapFailureToValue(403, false))
                    .compose(this::handleError)
                    // there's no actual content, result is always null
                    .map(result -> true)
                    .toObservable()
                    .compose(mRxLoader.makeObservableTransformer(ID_LOADER_COLLABORATOR_STATUS, force));
        }

        observable.subscribe(result -> {
            if (mIsCollaborator == null) {
                mIsCollaborator = result;
                if (mIsCollaborator) {
                    updateRightNavigationDrawer();
                }
            }
        }, error -> {});
    }
}
