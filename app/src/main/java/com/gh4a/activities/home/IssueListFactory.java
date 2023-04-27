package com.gh4a.activities.home;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.utils.ApiHelpers;

public class IssueListFactory extends FragmentFactory {
    private static final String QUERY = "is:%s is:%s %s:%s";

    private static final String STATE_KEY_SHOWING_CLOSED = "issue:showing_closed";

    private static final int[] TAB_TITLES = new int[] {
            R.string.created, R.string.assigned, R.string.mentioned, R.string.participating
    };

    private boolean mShowingClosed;
    private final String mLogin;
    private final boolean mIsPullRequest;
    private final IssueListFragment.SortDrawerHelper mDrawerHelper =
            new IssueListFragment.SortDrawerHelper();
    private SharedPreferences mPrefs;

    public IssueListFactory(HomeActivity activity, String userLogin, boolean pr,
            SharedPreferences prefs) {
        super(activity);
        mLogin = userLogin;
        mShowingClosed = false;
        mIsPullRequest = pr;
        mPrefs = prefs;

        String lastOrder = mPrefs.getString(getSortOrderPrefKey(), null);
        String lastDir = mPrefs.getString(getSortDirPrefKey(), null);
        if (lastOrder != null && lastDir != null) {
            mDrawerHelper.setSortMode(lastOrder, lastDir);
        }
    }

    @Override
    protected @StringRes int getTitleResId() {
        if (mShowingClosed) {
            return mIsPullRequest ? R.string.pull_requests_closed : R.string.issues_closed;
        } else {
            return mIsPullRequest ? R.string.pull_requests_open : R.string.issues_open;
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        final String action;
        if (position == 1) {
            action = "assignee";
        } else if (position == 2) {
            action = "mentions";
        } else if (position == 3) {
            action = "involves";
        } else {
            action = "author";
        }

        final String query = String.format(QUERY, mIsPullRequest ? "pr" : "issue",
                mShowingClosed ? ApiHelpers.IssueState.CLOSED : ApiHelpers.IssueState.OPEN,
                action, mLogin);

        return IssueListFragment.newInstance(query,
                mDrawerHelper.getSortMode(), mDrawerHelper.getSortOrder(),
                mShowingClosed ? ApiHelpers.IssueState.CLOSED : ApiHelpers.IssueState.OPEN,
                mIsPullRequest ? R.string.no_pull_requests_found : R.string.no_issues_found,
                true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int resIdState = mShowingClosed ?
                R.string.issues_menu_show_open : R.string.issues_menu_show_closed;
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, resIdState)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.actions)
                .setIcon(R.drawable.menu_overflow_horizontal)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:
                toggleStateFilter();
                return true;
            case Menu.FIRST + 1:
                mActivity.toggleToolDrawer();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int[] getToolDrawerMenuResIds() {
        return new int[] { IssueListFragment.SortDrawerHelper.getMenuResId() };
    }

    @Override
    protected void prepareToolDrawerMenu(Menu menu) {
        super.prepareToolDrawerMenu(menu);
        mDrawerHelper.updateMenuCheckState(menu);
    }

    @Override
    protected boolean onDrawerItemSelected(MenuItem item) {
        if (mDrawerHelper.handleItemSelection(item)) {
            mPrefs.edit()
                    .putString(getSortOrderPrefKey(), mDrawerHelper.getSortMode())
                    .putString(getSortDirPrefKey(), mDrawerHelper.getSortOrder())
                    .apply();
            reloadIssueList();
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_KEY_SHOWING_CLOSED, mShowingClosed);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        boolean showedClosed = state.getBoolean(STATE_KEY_SHOWING_CLOSED, false);
        if (mShowingClosed != showedClosed) {
            mShowingClosed = showedClosed;
            reloadIssueList();
            mActivity.invalidateTitle();
        }
    }

    private void reloadIssueList() {
        mActivity.invalidateFragments();
    }

    private void toggleStateFilter() {
        mShowingClosed = !mShowingClosed;
        reloadIssueList();
        mActivity.invalidateTitle();
        mActivity.supportInvalidateOptionsMenu();
    }

    private String getSortOrderPrefKey() {
        return mIsPullRequest ? "home_pr_list_sort_order" : "home_issue_list_sort_order";
    }

    private String getSortDirPrefKey() {
        return mIsPullRequest ? "home_pr_list_sort_dir" : "home_issue_list_sort_dir";
    }
}
