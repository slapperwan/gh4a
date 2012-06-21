package com.gh4a;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.gh4a.fragment.EventListFragment;
import com.gh4a.fragment.RepositoryIssueListFragment;
import com.gh4a.fragment.UserFragment;

public class UserNewActivity extends BaseSherlockFragmentActivity {

    private static final int NUM_ITEMS = 5;
    private String mUserLogin;
    private String mUserName;
    private UserAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_new);
        
        mUserLogin = "slapperwan";
        mUserName = null;
        
        mActionBar = getSupportActionBar();
        mAdapter = new UserAdapter(getSupportFragmentManager());
        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int arg0) {
                Log.d("ViewPager", "onPageSelected: " + arg0);
                mActionBar.getTabAt(arg0).select();
                }
        });
        
        mActionBar.setTitle(mUserLogin);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.about)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.user_news_feed)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.user_your_actions)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.issues)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 3 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.pull_requests)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 4 + "", mPager));
        mActionBar.addTab(tab);
    }
    
    public class UserAdapter extends FragmentStatePagerAdapter {

        public UserAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 0) {
                return UserFragment.newInstance(UserNewActivity.this.mUserLogin,
                        UserNewActivity.this.mUserName);
            }
            else if (position == 1) {
                return EventListFragment.newInstance(UserNewActivity.this.mUserLogin, true);
            }
            else if (position == 2) {
                return EventListFragment.newInstance(UserNewActivity.this.mUserLogin, false);
            }
            else if (position == 3) {
                Map<String, String> filterData = new HashMap<String, String>();
                filterData.put("filter", "subscribed");
                return RepositoryIssueListFragment.newInstance(filterData);
            }
            else {
                return UserFragment.newInstance(UserNewActivity.this.mUserLogin,
                        UserNewActivity.this.mUserName);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //
        }
        
    }
    
    public class TabListener<T extends SherlockFragmentActivity> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private ViewPager mPager;

        public TabListener(SherlockFragmentActivity activity, String tag, ViewPager pager) {
            mActivity = activity;
            mTag = tag;
            mPager = pager;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            mPager.setCurrentItem(Integer.parseInt(mTag));
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }
}
