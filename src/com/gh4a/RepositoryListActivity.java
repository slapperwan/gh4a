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
import com.gh4a.fragment.RepositoryListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;

public class RepositoryListActivity extends BaseSherlockFragmentActivity {

    public String mUserLogin;
    public String mUserType;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private int tabCount;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserType = data.getString(Constants.User.USER_TYPE);

        if (Constants.User.USER_TYPE_ORG.equals(mUserType)) {
            tabCount = 1;
        }
        else {
            tabCount = 2;
        }
        
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
        
        mActionBar.setTitle(mUserLogin);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.user_pub_repos)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        if (tabCount == 2) {
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_watched_repos)
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
            if (position == 0) {
                return RepositoryListFragment.newInstance(RepositoryListActivity.this.mUserLogin,
                        RepositoryListActivity.this.mUserType);
            }
            else if (position == 1) {
                return WatchedRepositoryListFragment.newInstance(RepositoryListActivity.this.mUserLogin);
            }
            else {
                return RepositoryListFragment.newInstance(RepositoryListActivity.this.mUserLogin,
                        RepositoryListActivity.this.mUserType);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openUserInfoActivity(this, mUserLogin, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;     
            default:
                return true;
        }
    }
}
