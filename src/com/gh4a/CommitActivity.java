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

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
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
import com.gh4a.fragment.CommitFragment;
import com.gh4a.fragment.CommitNoteFragment;

public class CommitActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    private String mObjectSha;
    private ThisPageAdapter mAdapter;
    private ActionBar mActionBar;
    private ViewPager mPager;
    private int tabCount;
    public ProgressDialog mProgressDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        mObjectSha = data.getString(Constants.Object.OBJECT_SHA);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.view_pager);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(getResources().getQuantityString(R.plurals.commit, 1) + " " + mObjectSha.substring(0, 7));
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
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
        
        tabCount = 2;
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.commits)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        if (tabCount == 2) {
            tab = mActionBar
                    .newTab()
                    .setText(R.string.issue_comments)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
            mActionBar.addTab(tab);
        }
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
            if (position == 1) {
                return CommitNoteFragment.newInstance(mRepoOwner, mRepoName, mObjectSha);
            }
            else {
                return CommitFragment.newInstance(mRepoOwner, mRepoName, mObjectSha);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.download_menu, menu);
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.getItem(0).setIcon(R.drawable.download_dark);
            menu.getItem(1).setIcon(R.drawable.web_site_dark);
            menu.getItem(2).setIcon(R.drawable.social_share_dark);
        }
        
        menu.removeItem(R.id.download);
        menu.removeItem(R.id.search);

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        String diffUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName + "/commit/" + mObjectSha;
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openRepositoryInfoActivity(this, mRepoOwner, mRepoName, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;   
            case R.id.browser:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(diffUrl));
                startActivity(browserIntent);
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Commit #" + mObjectSha.substring(0, 7) + " at " + mRepoOwner + "/" + mRepoName);
                shareIntent.putExtra(Intent.EXTRA_TEXT,  "Commit #" + mObjectSha.substring(0, 7) 
                        + " at " + mRepoOwner + "/" + mRepoName + " " + diffUrl);
                shareIntent = Intent.createChooser(shareIntent, "Share");
                startActivity(shareIntent);
                return true;
            default:
                return true;
        }
    }
}