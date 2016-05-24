package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.IssueListFragment;

import java.util.HashMap;
import java.util.Map;

public class IssueListFactory extends FragmentFactory {
    private static final String QUERY = "is:%s is:%s %s:%s";

    private static final String STATE_KEY_STATE = "issue:state";

    private static final int[] TAB_TITLES = new int[] {
            R.string.created, R.string.assigned, R.string.mentioned
    };

    private String mState;
    private String mLogin;
    private boolean mIsPullRequest;
    private IssueListFragment.SortDrawerHelper mDrawerHelper =
            new IssueListFragment.SortDrawerHelper();
    private int[] mHeaderColors;

    public IssueListFactory(HomeActivity activity, String userLogin, boolean pr) {
        super(activity);
        mLogin = userLogin;
        mState = Constants.Issue.STATE_OPEN;
        mIsPullRequest = pr;
    }

    @Override
    protected int getTitleResId() {
        if (Constants.Issue.STATE_OPEN.equals(mState)) {
            return mIsPullRequest ? R.string.pull_requests_open : R.string.issues_open;
        } else {
            return mIsPullRequest ? R.string.pull_requests_closed : R.string.issues_closed;
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
                mState, action, mLogin));

        return IssueListFragment.newInstance(filterData,
                Constants.Issue.STATE_CLOSED.equals(mState),
                mIsPullRequest ? R.string.no_pull_requests_found : R.string.no_issues_found,
                true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int resIdState = Constants.Issue.STATE_OPEN.equals(mState) ?
                R.string.issues_menu_show_closed : R.string.issues_menu_show_open;
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
        outState.putString(STATE_KEY_STATE, mState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        String newState = state.getString(STATE_KEY_STATE);
        if (!TextUtils.equals(mState, newState)) {
            mState = newState;
            reloadIssueList();
            updateHeaderColor();
            mActivity.invalidateTitle();
        }
    }

    private void reloadIssueList() {
        mActivity.invalidateFragments();
    }

    private void toggleStateFilter() {
        mState = Constants.Issue.STATE_CLOSED.equals(mState)
                ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED;
        reloadIssueList();
        updateHeaderColor();
        mActivity.invalidateTitle();
    }

    private void updateHeaderColor() {
        boolean showingClosed = Constants.Issue.STATE_CLOSED.equals(mState);
        mHeaderColors = new int[] {
            showingClosed ? R.attr.colorIssueClosed : R.attr.colorIssueOpen,
            showingClosed ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark
        };
        mActivity.invalidateTabs();
    }
}
