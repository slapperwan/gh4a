package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.utils.ApiHelpers;

import java.util.HashMap;
import java.util.Map;

public class IssueListFactory extends FragmentFactory {
    private static final String QUERY = "is:%s is:%s %s:%s";

    private static final String STATE_KEY_SHOWING_CLOSED = "issue:showing_closed";

    private static final int[] TAB_TITLES = new int[] {
            R.string.created, R.string.assigned, R.string.mentioned
    };

    private boolean mShowingClosed;
    private String mLogin;
    private boolean mIsPullRequest;
    private IssueListFragment.SortDrawerHelper mDrawerHelper =
            new IssueListFragment.SortDrawerHelper();
    private int[] mHeaderColors;

    public IssueListFactory(HomeActivity activity, String userLogin, boolean pr) {
        super(activity);
        mLogin = userLogin;
        mShowingClosed = false;
        mIsPullRequest = pr;
    }

    @Override
    protected int getTitleResId() {
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
    protected int[] getHeaderColors() {
        return mHeaderColors;
    }

    @Override
    protected Fragment getFragment(int position) {
        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", mDrawerHelper.getSortMode());
        filterData.put("order", mDrawerHelper.getSortDirection());

        final String action;
        if (position == 1) {
            action = "assignee";
        } else if (position == 2) {
            action = "mentions";
        } else {
            action = "author";
        }

        filterData.put("q", String.format(QUERY, mIsPullRequest ? "pr" : "issue",
                mShowingClosed ? ApiHelpers.IssueState.CLOSED : ApiHelpers.IssueState.OPEN,
                action, mLogin));

        return IssueListFragment.newInstance(filterData, mShowingClosed,
                mIsPullRequest ? R.string.no_pull_requests_found : R.string.no_issues_found,
                true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int resIdState = mShowingClosed ?
                R.string.issues_menu_show_open : R.string.issues_menu_show_closed;
        MenuItem item = menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, resIdState);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        item = menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, R.string.actions)
                .setIcon(R.drawable.overflow);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

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
    protected boolean onDrawerItemSelected(MenuItem item) {
        if (mDrawerHelper.handleItemSelection(item)) {
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
            updateHeaderColor();
            mActivity.invalidateTitle();
        }
    }

    private void reloadIssueList() {
        mActivity.invalidateFragments();
    }

    private void toggleStateFilter() {
        mShowingClosed = !mShowingClosed;
        reloadIssueList();
        updateHeaderColor();
        mActivity.invalidateTitle();
        mActivity.supportInvalidateOptionsMenu();
    }

    private void updateHeaderColor() {
        mHeaderColors = new int[] {
            mShowingClosed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen,
            mShowingClosed ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark
        };
        mActivity.invalidateTabs();
    }
}
