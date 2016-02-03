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

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.RepositoryListContainerFragment;

public class RepositoryListActivity extends BaseActivity implements
        RepositoryListContainerFragment.Callback {
    private String mUserLogin;
    private String mUserType;
    private RepositoryListContainerFragment mFragment;
    private RepositoryListContainerFragment.FilterDrawerHelper mDrawerHelper;

    private static final String STATE_KEY_INDEX = "selectedIndex";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserType = data.getString(Constants.User.TYPE);

        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState == null) {
            mFragment = RepositoryListContainerFragment.newInstance(mUserLogin, mUserType);
            fm.beginTransaction().add(R.id.content_container, mFragment).commit();
        } else {
            mFragment = (RepositoryListContainerFragment) fm.findFragmentById(R.id.details);
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.user_pub_repos);
        actionBar.setSubtitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setChildScrollDelegate(mFragment);
    }

    @Override
    protected int[] getRightNavigationDrawerMenuResources() {
        mDrawerHelper = RepositoryListContainerFragment.FilterDrawerHelper.create(this,
                mUserLogin, mUserType);
        return mDrawerHelper.getMenuResIds();
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        super.onNavigationItemSelected(item);
        String type = mDrawerHelper.handleSelectionAndGetFilterType(item);
        if (type != null) {
            mFragment.setFilterType(type);
            super.supportInvalidateOptionsMenu();
            return true;
        }
        return false;
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void initiateFilter() {
        toggleRightSideDrawer();
    }

    @Override
    public void onRefresh() {
        mFragment.refresh();
        refreshDone();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        // happens when load is done; we ignore it as we don't want to close the IME in that case
    }
}
