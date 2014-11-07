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
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.ForkListFragment;

public class ForkListActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        String repoOwner = data.getString(Constants.Repository.OWNER);
        String repoName = data.getString(Constants.Repository.NAME);

        if (savedInstanceState == null) {
            Fragment fragment = ForkListFragment.newInstance(repoOwner, repoName);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_container, fragment)
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.repo_forks);
        actionBar.setSubtitle(repoOwner + "/" + repoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }
}
