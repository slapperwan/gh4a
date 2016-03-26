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

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.FollowersFollowingListFragment;
import com.gh4a.utils.IntentUtils;

public class FollowerFollowingListActivity extends FragmentContainerActivity {
    private String mUserLogin;
    private boolean mShowFollowers;

    public static final String EXTRA_SHOW_FOLLOWERS = "show_followers";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mShowFollowers ? R.string.user_followers : R.string.user_following);
        actionBar.setSubtitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mShowFollowers = extras.getBoolean(EXTRA_SHOW_FOLLOWERS);
        mUserLogin = extras.getString(Constants.User.LOGIN);
    }

    @Override
    protected Fragment onCreateFragment() {
        return FollowersFollowingListFragment.newInstance(mUserLogin, mShowFollowers);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getUserActivityIntent(this, mUserLogin);
    }
}