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

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.ContributorListFragment;

public class ContributorListActivity extends BaseSherlockFragmentActivity {
    private String mUserLogin;
    private String mRepoName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle extras = getIntent().getExtras();
        mUserLogin = extras.getString(Constants.Repository.REPO_OWNER);
        mRepoName = extras.getString(Constants.Repository.REPO_NAME);
        
        if (savedInstanceState == null) {
            ContributorListFragment fragment = ContributorListFragment.newInstance(
                    mUserLogin, mRepoName);
            getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, fragment)
                    .commit();
        }
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.repo_contributors);
        actionBar.setSubtitle(mUserLogin + "/" + mRepoName);
    }
    
    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openRepositoryInfoActivity(this,
                mUserLogin, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
}