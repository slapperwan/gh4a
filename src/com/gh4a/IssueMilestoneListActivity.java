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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.IssueMilestoneListFragment;

public class IssueMilestoneListActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private ThisPageAdapter mAdapter;
    private int tabCount;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.view_pager);
        setUpActionBar();
        
        mRepoOwner = getIntent().getExtras().getString(Constants.Repository.REPO_OWNER);
        mRepoName = getIntent().getExtras().getString(Constants.Repository.REPO_NAME);

        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setTitle(R.string.issue_manage_milestones);
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        tabCount = 2;
        
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
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.open)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.closed)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab);
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
                return IssueMilestoneListFragment.newInstance(mRepoOwner, mRepoName, "open");
            }
            else if (position == 1) {
                return IssueMilestoneListFragment.newInstance(mRepoOwner, mRepoName, "closed");
            }
            return null;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (isAuthorized()) {
            menu.clear();
            MenuInflater inflater = getSupportMenuInflater();
            inflater.inflate(R.menu.create_new, menu);
        }
        return true;
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName,
                        Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.create_new:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueMilestoneCreateActivity.class);
                    intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                    intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                    startActivity(intent);
                }
                else {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            default:
                return true;
        }
    }
}