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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.IssueListFragment;
import com.gh4a.fragment.RepositoryIssueListFragment;
import com.gh4a.loader.CollaboratorListLoader;
import com.gh4a.loader.IsCollaboratorLoader;
import com.gh4a.loader.LabelListLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.MilestoneListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.ToastUtils;

import org.eclipse.egit.github.core.Label;
import org.eclipse.egit.github.core.Milestone;
import org.eclipse.egit.github.core.User;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueListMineActivity extends IssueListBaseActivity {
    private RepositoryIssueListFragment mRepositoryIssueListFragment;
    private String mState;

    private static final int[] TITLES = new int[] {
        R.string.created, R.string.assigned, R.string.mentioned
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        mSortMode = SORT_MODE_CREATED;
        mSortAscending = false;
        mState = Constants.Issue.STATE_OPEN;
        updateSortDrawerItemState(ITEM_SORT_CREATED_DESC);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getString(R.string.issues_with_state,
                StringUtils.capitalizeFirstChar(mState)));
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        Map<String, String> filterData = new HashMap<String, String>();
        filterData.put("sort", mSortMode);
        filterData.put("direction", mSortAscending ? "asc" : "desc");
        filterData.put(Constants.Issue.STATE, mState);

        if (position == 1) {
            filterData.put("filter", "assigned");
            mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
            return mRepositoryIssueListFragment;
        } else if (position == 2) {
            filterData.put("filter", "mentioned");
            mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
            return mRepositoryIssueListFragment;
        } else {
            filterData.put("filter", "created");
            mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
            return mRepositoryIssueListFragment;
        }
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
                R.string.closed : R.string.open;
        MenuItem item = menu.add(Menu.NONE, 2, Menu.NONE, resIdState);
        MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 2) {
            filterState(item.getTitle().toString());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected ListAdapter getNavigationDrawerAdapter() {
        mDrawerItems = new ArrayList<DrawerAdapter.Item>(DRAWER_ITEMS);
        mDrawerAdapter = new DrawerAdapter(this, mDrawerItems);
        return mDrawerAdapter;
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getUserActivityIntent(this, Gh4Application.get(this).getAuthLogin());
    }

    @Override
    public void reloadIssueList() {
        mRepositoryIssueListFragment = null;
        invalidateFragments();
    }

    public void filterState(String state) {
        mState = state.toLowerCase();
        reloadIssueList();

        getSupportActionBar().setTitle(getString(R.string.issues_with_state,
                StringUtils.capitalizeFirstChar(mState)));
    }

}