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
import android.text.TextUtils;

import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.GistListFragment;
import com.gh4a.utils.IntentUtils;

public class GistListActivity extends BasePagerActivity {
    private String mUserLogin;
    private boolean mIsSelf;

    private static final int[] TITLES_SELF = new int[] {
        R.string.mine, R.string.starred
    };
    private static final int[] TITLES_OTHER = new int[] {
        R.string.gists
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mUserLogin = getIntent().getExtras().getString(Constants.User.LOGIN);
        mIsSelf = TextUtils.equals(mUserLogin, Gh4Application.get().getAuthLogin());

        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.gists);
        actionBar.setSubtitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mIsSelf ? TITLES_SELF : TITLES_OTHER;
    }

    @Override
    protected Fragment getFragment(int position) {
        return GistListFragment.newInstance(mUserLogin, position == 1);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getUserActivityIntent(this, mUserLogin);
    }
}