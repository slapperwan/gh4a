package com.gh4a;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ViewGroup;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.DownloadBranchesFragment;
import com.gh4a.fragment.DownloadTagsFragment;
import com.gh4a.fragment.DownloadsFragment;

public class DownloadsActivity extends BaseSherlockFragmentActivity {

    private String mRepoOwner;
    private String mRepoName;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private int tabCount;
    
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
        
        mActionBar.setTitle(R.string.downloads);
        mActionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.packages)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.repo_branches)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.repo_tags)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
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
                return DownloadsFragment.newInstance(mRepoOwner, mRepoName);
            }
            else if (position == 1) {
                return DownloadBranchesFragment.newInstance(mRepoOwner, mRepoName);
            }
            else if (position == 2) {
                return DownloadTagsFragment.newInstance(mRepoOwner, mRepoName);
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
                finish();
                return true;     
            default:
                return true;
        }
    }
}
