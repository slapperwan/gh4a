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

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.FollowersFollowingListFragment;
import com.gh4a.utils.IntentUtils;

public class FollowerFollowingListActivity extends LoadingFragmentPagerActivity {

    private String mUserLogin;

    private static final int[] TITLES = new int[] {
        R.string.user_followers, R.string.user_following
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (!data.getBoolean("FIND_FOLLOWERS", true)) {
            actionBar.selectTab(actionBar.getTabAt(1));
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        return FollowersFollowingListFragment.newInstance(
                FollowerFollowingListActivity.this.mUserLogin, position == 0);
    }

    @Override
    protected void navigateUp() {
        IntentUtils.openUserInfoActivity(this, mUserLogin, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}