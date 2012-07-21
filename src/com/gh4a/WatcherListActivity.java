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
package com.gh4a;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.ForkListFragment;
import com.gh4a.fragment.WatcherListFragment;

public class WatcherListActivity extends BaseSherlockFragmentActivity  {

    private String mRepoOwner;
    private String mRepoName;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private int tabCount;
    private int mPos;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mPos = data.getInt("pos");
        
        tabCount = 2;
        
        mActionBar = getSupportActionBar();
        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int position) {
                mActionBar.setSelectedNavigationItem(position);
            }
        });
        
        mActionBar.setTitle(mRepoName);
        mActionBar.setSubtitle(mRepoOwner);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.repo_watchers)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, mPos == 0);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.repo_forks)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, mPos == 1);
    }

    public class ThisPageAdapter extends FragmentStatePagerAdapter {

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return tabCount;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 0) {
                return WatcherListFragment.newInstance(mRepoOwner, mRepoName);
            }
            else if (position == 1) {
                return ForkListFragment.newInstance(mRepoOwner, mRepoName);
            }
            
            return null;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;     
            default:
                return true;
        }
    }
}
