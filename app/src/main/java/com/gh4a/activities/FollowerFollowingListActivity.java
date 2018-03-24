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
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.FollowersFollowingListFragment;

public class FollowerFollowingListActivity extends FragmentContainerActivity {

    private static final String EXTRA_USER = "user";
    private static final String EXTRA_SHOW_FOLLOWERS = "show_followers";

    public static Intent makeIntent(Context context, String user, boolean showFollowers) {
        return new Intent(context, FollowerFollowingListActivity.class)
                .putExtra(EXTRA_USER, user)
                .putExtra(EXTRA_SHOW_FOLLOWERS, showFollowers);
    }

    private String mUserLogin;
    private boolean mShowFollowers;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(mShowFollowers ? R.string.user_followers : R.string.user_following);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mUserLogin;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mShowFollowers = extras.getBoolean(EXTRA_SHOW_FOLLOWERS);
        mUserLogin = extras.getString(EXTRA_USER);
    }

    @Override
    protected Fragment onCreateFragment() {
        return FollowersFollowingListFragment.newInstance(mUserLogin, mShowFollowers);
    }

    @Override
    protected Intent navigateUp() {
        return UserActivity.makeIntent(this, mUserLogin);
    }
}