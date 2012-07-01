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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.IssueListByCommentsFragment;
import com.gh4a.fragment.IssueListBySubmittedFragment;
import com.gh4a.fragment.IssueListByUpdatedFragment;

public class IssueListActivity extends BaseSherlockFragmentActivity
    implements OnClickListener {

    private String mRepoOwner;
    private String mRepoName;
    private String mState;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private int tabCount;
    private Map<String, String> mFilterData;
    private Button mBtnSort;
    private Button mBtnFilterByLabels;
    private Button mBtnFilterByMilestone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.issue_list_view_pager);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mState = data.getString(Constants.Issue.ISSUE_STATE);
        int position = data.getInt("position");
        
        mFilterData = new HashMap<String, String>();
        Iterator<String> filter = data.keySet().iterator();
        while (filter.hasNext()) {
            String key = filter.next();
            if (!Constants.Repository.REPO_OWNER.equals(key)
                    && !Constants.Repository.REPO_NAME.equals(key)) {
                
                if (key.equals("position")) {
                    mFilterData.put(key, String.valueOf(data.getInt(key)));
                }
                else {
                    mFilterData.put(key, data.getString(key));
                }
            }
        }
        
        tabCount = 3;
        
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
        
        mActionBar.setTitle(R.string.issues);
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.issues_submitted)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, position == 0);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_updated)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, position == 1);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues_comments)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab, position == 2);
        
        mBtnSort = (Button) findViewById(R.id.btn_sort);
        mBtnSort.setOnClickListener(this);
        
        mBtnFilterByLabels = (Button) findViewById(R.id.btn_labels);
        mBtnFilterByLabels.setOnClickListener(this);
        
        mBtnFilterByMilestone = (Button) findViewById(R.id.btn_milestone);
        mBtnFilterByMilestone.setOnClickListener(this);
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
                return IssueListBySubmittedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            
            else if (position == 1) {
                return IssueListByUpdatedFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            
            else if (position == 2) {
                return IssueListByCommentsFragment.newInstance(mRepoOwner, mRepoName, mFilterData);
            }
            
            return null;
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.issues_menu, menu);
        if ("open".equals(mState)) {
            menu.removeItem(R.id.view_open_issues);
        }
        else {
            menu.removeItem(R.id.view_closed_issues);
        }
        
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.view_open_issues:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName, 
                        Constants.Issue.ISSUE_STATE_OPEN, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.view_closed_issues:
                getApplicationContext().openIssueListActivity(this, mRepoOwner, mRepoName,
                        Constants.Issue.ISSUE_STATE_CLOSED, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.create_issue:
                if (isAuthorized()) {
                    Intent intent = new Intent().setClass(this, IssueCreateActivity.class);
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
            case R.id.view_labels:
                Intent intent = new Intent().setClass(IssueListActivity.this, IssueLabelListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;
            case R.id.view_milestones:
                intent = new Intent().setClass(IssueListActivity.this, IssueMilestoneListActivity.class);
                intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                startActivity(intent);
                return true;    
                
            default:
                return true;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.btn_sort:
            Intent intent = new Intent().setClass(this, IssueListActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Issue.ISSUE_STATE, mState);
            intent.putExtra("position", mPager.getCurrentItem());
            String direction = mFilterData.get("direction");
            if ("desc".equals(direction) || direction == null) {
                intent.putExtra("direction", "asc");
            }
            else {
                intent.putExtra("direction", "desc");
            }
            
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            break;

        default:
            break;
        }
        
    }
}
