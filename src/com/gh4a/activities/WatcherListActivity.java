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

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.ForkListFragment;
import com.gh4a.fragment.StargazerListFragment;
import com.gh4a.fragment.WatcherListFragment;

public class WatcherListActivity extends LoadingFragmentPagerActivity {

    private String mRepoOwner;
    private String mRepoName;
    
    private static final int[] TITLES = new int[] {
        R.string.repo_stargazers, R.string.repo_watchers, R.string.repo_forks
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.selectTab(actionBar.getTabAt(data.getInt("pos")));
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0: return StargazerListFragment.newInstance(mRepoOwner, mRepoName);
            case 1: return WatcherListFragment.newInstance(mRepoOwner, mRepoName);
            case 2: return ForkListFragment.newInstance(mRepoOwner, mRepoName);
        }
            
        return null;
    }
    
    @Override
    protected void navigateUp() {
        finish();
    }
}
