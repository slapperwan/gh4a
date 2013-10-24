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
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.ForkListFragment;
import com.gh4a.fragment.StargazerListFragment;
import com.gh4a.fragment.WatcherListFragment;

public class WatcherListActivity extends BaseSherlockFragmentActivity  {

    private String mRepoOwner;
    private String mRepoName;
    
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
        
        setContentView(R.layout.view_pager);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setupPager(new ThisPageAdapter(getSupportFragmentManager()), new int[] {
            R.string.repo_stargazers, R.string.repo_watchers, R.string.repo_forks
        });
        actionBar.selectTab(actionBar.getTabAt(data.getInt("pos")));
    }

    public class ThisPageAdapter extends FragmentStatePagerAdapter {

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 0) {
                return StargazerListFragment.newInstance(mRepoOwner, mRepoName);
            }
            else if (position == 1) {
                return WatcherListFragment.newInstance(mRepoOwner, mRepoName);
            }
            else if (position == 2) {
                return ForkListFragment.newInstance(mRepoOwner, mRepoName);
            }
            
            return null;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    protected void navigateUp() {
        finish();
    }
}
