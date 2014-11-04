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
import android.support.v7.app.ActionBar;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.FollowersFollowingListFragment;
import com.gh4a.utils.IntentUtils;

public class FollowerFollowingListActivity extends BaseActivity {
    private String mUserLogin;

    public static final String EXTRA_SHOW_FOLLOWERS = "show_followers";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        boolean showFollowers = data.getBoolean(EXTRA_SHOW_FOLLOWERS);
        mUserLogin = data.getString(Constants.User.LOGIN);

        if (savedInstanceState == null) {
            Fragment fragment = FollowersFollowingListFragment.newInstance(mUserLogin, showFollowers);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_container, fragment)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(showFollowers ? R.string.user_followers : R.string.user_following);
        actionBar.setSubtitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getUserActivityIntent(this, mUserLogin);
    }
}