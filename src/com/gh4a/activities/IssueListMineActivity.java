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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.RepositoryIssueListFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IssueListMineActivity extends IssueListBaseActivity {
    private static final String QUERY = "is:%s is:%s %s:%s";
    private RepositoryIssueListFragment mRepositoryIssueListFragment;
    private String mState;
    private String mLogin;
    private String mType; // issue or pr

    private static final int[] TITLES = new int[] {
        R.string.created, R.string.assigned, R.string.mentioned
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mType = data.getString("type");
        mLogin = Gh4Application.get().getAuthLogin();
        mSortMode = SORT_MODE_CREATED;
        mSortAscending = false;
        mState = Constants.Issue.STATE_OPEN;
        updateSortDrawerItemState(ITEM_SORT_CREATED_DESC);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("issue".equals(mType) ?
                R.string.issues_open : R.string.pull_requests_open);
        actionBar.setDisplayHomeAsUpEnabled(true);
        updateHeaderColor();
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        Map<String, String> filterData = new HashMap<>();
        filterData.put("sort", mSortMode);
        filterData.put("order", mSortAscending ? "asc" : "desc");

        String action;
        if (position == 1) {
            action = "assignee";
        } else if (position == 2) {
            action = "mentions";
        } else {
            action = "author";
        }

        filterData.put("q", String.format(QUERY, mType, mState, action, mLogin));

        mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
        return mRepositoryIssueListFragment;
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        if (object instanceof RepositoryIssueListFragment) {
            if (object != mRepositoryIssueListFragment) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        int resIdState = Constants.Issue.STATE_OPEN.equals(mState) ?
                R.string.issues_menu_show_closed : R.string.issues_menu_show_open;
        MenuItem item = menu.add(Menu.NONE, Menu.FIRST + 1, Menu.NONE, resIdState);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST + 1) {
            toggleStateFilter();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected ListAdapter getNavigationDrawerAdapter() {
        mDrawerItems = new ArrayList<>(DRAWER_ITEMS);
        mDrawerAdapter = new DrawerAdapter(this, mDrawerItems);
        return mDrawerAdapter;
    }

    @Override
    protected Intent navigateUp() {
        return getToplevelActivityIntent();
    }

    @Override
    public void reloadIssueList() {
        mRepositoryIssueListFragment = null;
        invalidateFragments();
    }

    private void toggleStateFilter() {
        mState = Constants.Issue.STATE_CLOSED.equals(mState)
                ? Constants.Issue.STATE_OPEN : Constants.Issue.STATE_CLOSED;
        reloadIssueList();
        updateHeaderColor();

        int titleResId = Constants.Issue.STATE_CLOSED.equals(mState)
                ? R.string.issues_closed : R.string.issues_open;

        if ("pr".equals(mType)) {
            titleResId = Constants.Issue.STATE_CLOSED.equals(mState)
                    ? R.string.pull_requests_closed : R.string.pull_requests_open;
        }

        getSupportActionBar().setTitle(titleResId);
    }

    private void updateHeaderColor() {
        boolean showingClosed = Constants.Issue.STATE_CLOSED.equals(mState);
        int headerColorAttrId = showingClosed
                ? R.attr.colorIssueClosed : R.attr.colorIssueOpen;
        int statusBarColorAttrId = showingClosed
                ? R.attr.colorIssueClosedDark : R.attr.colorIssueOpenDark;
        transitionHeaderToColor(headerColorAttrId, statusBarColorAttrId);
    }
}