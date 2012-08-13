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
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserType = data.getString(Constants.User.USER_TYPE);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.view_pager);
        
        if (Constants.User.USER_TYPE_ORG.equals(mUserType)) {
            tabCount = 1;
        }
        else if (mUserLogin.equals(getAuthLogin())) {
            tabCount = 7;
        }
        else {
            tabCount = 5;
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
        mActionBar.setSubtitle(R.string.user_pub_repos);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.user_all_repos)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        if (!Constants.User.USER_TYPE_ORG.equals(mUserType)) {
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_stars_repos)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
            mActionBar.addTab(tab);
            
            if (mUserLogin.equals(getAuthLogin())) {
                tab = mActionBar
                        .newTab()
                        .setText(R.string.user_public_repos)
                        .setTabListener(
                                new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
                mActionBar.addTab(tab);
                
                tab = mActionBar
                        .newTab()
                        .setText(R.string.user_private_repos)
                        .setTabListener(
                                new TabListener<SherlockFragmentActivity>(this, 3 + "", mPager));
                mActionBar.addTab(tab);
            }
            
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_sources_repos)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, (tabCount - 3) + "", mPager));
            mActionBar.addTab(tab);
            
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_forks_repos)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, (tabCount - 2) + "", mPager));
            mActionBar.addTab(tab);
            
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_member_repos)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, (tabCount - 1) + "", mPager));
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
            //Repo type all, owner, public, private, member
            if (position == 0) {
                return RepositoryListFragment.newInstance(mUserLogin, mUserType, "all");
            }
            else if (position == 1) {
                return WatchedRepositoryListFragment.newInstance(mUserLogin);
            }
            else if (position == 2) {
                if (mUserLogin.equals(getAuthLogin())) {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "public");
                }
                else {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "sources");
                }
            }
            else if (position == 3) {
                if (mUserLogin.equals(getAuthLogin())) {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "private");
                }
                else {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "forks");
                }
            }
            else if (position == 4) {
                if (mUserLogin.equals(getAuthLogin())) {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "sources");
                }
                else {
                    return RepositoryListFragment.newInstance(mUserLogin, mUserType, "member");
                }
            }
            else if (position == 5) {
                return RepositoryListFragment.newInstance(mUserLogin, mUserType, "forks");
            }
            else if (position == 6) {
                return RepositoryListFragment.newInstance(mUserLogin, mUserType, "member");
            }
            else {
                return RepositoryListFragment.newInstance(mUserLogin, mUserType, "all");
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
