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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.RepositoryListContainerFragment;

public class RepositoryListActivity extends FragmentContainerActivity implements
        RepositoryListContainerFragment.Callback {

    private static final String EXTRA_USER = "user";
    private static final String EXTRA_IS_ORG = "is_org";
    private static final String EXTRA_FILTER_TYPE = "filter_type";

    public static Intent makeIntent(Context context, String user, boolean userIsOrg) {
        return makeIntent(context, user, userIsOrg, null);
    }

    public static Intent makeIntent(Context context, String user, boolean userIsOrg,
            String defaultFilter) {
        return new Intent(context, RepositoryListActivity.class)
                .putExtra(EXTRA_USER, user)
                .putExtra(EXTRA_IS_ORG, userIsOrg)
                .putExtra(EXTRA_FILTER_TYPE, defaultFilter);
    }

    private String mUserLogin;
    private boolean mUserIsOrg;
    private String mFilterType;
    private RepositoryListContainerFragment mFragment;
    private RepositoryListContainerFragment.FilterDrawerHelper mFilterDrawerHelper;
    private RepositoryListContainerFragment.SortDrawerHelper mSortDrawerHelper;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragment = (RepositoryListContainerFragment) getFragment();
        mSortDrawerHelper.setFilterType(mFragment.getFilterType());
        updateRightNavigationDrawer();
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.user_pub_repos);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mUserLogin;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(EXTRA_USER);
        mUserIsOrg = data.getBoolean(EXTRA_IS_ORG);
        mFilterType = data.getString(EXTRA_FILTER_TYPE);

        mFilterDrawerHelper = RepositoryListContainerFragment.FilterDrawerHelper.create(
                mUserLogin, mUserIsOrg);
        mSortDrawerHelper = new RepositoryListContainerFragment.SortDrawerHelper();
        if (mFilterType != null) {
            mSortDrawerHelper.setFilterType(mFilterType);
        }
    }

    @Override
    protected Fragment onCreateFragment() {
        return RepositoryListContainerFragment.newInstance(mUserLogin, mUserIsOrg, mFilterType);
    }

    @Override
    protected int[] getRightNavigationDrawerMenuResources() {
        int sortMenuResId = mSortDrawerHelper.getMenuResId();
        int filterMenuResId = mFilterDrawerHelper.getMenuResId();
        if (sortMenuResId == 0) {
            return new int[] { filterMenuResId };
        } else {
            return new int[] { sortMenuResId, filterMenuResId };
        }
    }

    @Override
    protected void onPrepareRightNavigationDrawerMenu(Menu menu) {
        if (mFragment != null) {
            mFilterDrawerHelper.selectFilterType(menu, mFragment.getFilterType());
            mSortDrawerHelper.selectSortType(menu, mFragment.getSortOrder(),
                    mFragment.getSortDirection(), false);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        super.onNavigationItemSelected(item);
        String type = mFilterDrawerHelper.handleSelectionAndGetFilterType(item);
        if (type != null) {
            mFragment.setFilterType(type);
            super.supportInvalidateOptionsMenu();
            updateRightNavigationDrawer();
            return true;
        }
        String[] sortOrderAndDirection = mSortDrawerHelper.handleSelectionAndGetSortOrder(item);
        if (sortOrderAndDirection != null) {
            mFragment.setSortOrder(sortOrderAndDirection[0], sortOrderAndDirection[1]);
            updateRightNavigationDrawer();
            return true;
        }
        return false;
    }

    @Override
    public void initiateFilter() {
        toggleRightSideDrawer();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        // happens when load is done; we ignore it as we don't want to close the IME in that case
    }

    @Override
    protected Intent navigateUp() {
        return UserActivity.makeIntent(this, mUserLogin);
    }
}
