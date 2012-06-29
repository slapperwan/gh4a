package com.gh4a;

import java.util.HashMap;
import java.util.Map;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.gh4a.fragment.PrivateEventListFragment;
import com.gh4a.fragment.PublicEventListFragment;
import com.gh4a.fragment.RepositoryIssueListFragment;
import com.gh4a.fragment.UserFragment;

public class UserActivity extends BaseSherlockFragmentActivity {

    public String mUserLogin;
    public String mUserName;
    private UserAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private boolean isLoginUserPage;
    private int tabCount;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserName = data.getString(Constants.User.USER_NAME);

        isLoginUserPage = mUserLogin.equals(getAuthLogin());
        
        if (isLoginUserPage) {
            tabCount = 4;
        }
        else {
            tabCount = 2;
        }
        
        mActionBar = getSupportActionBar();
        mAdapter = new UserAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int position) {
                Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onPageSelected " + position);
                mActionBar.setSelectedNavigationItem(position);
                invalidateOptionsMenu();
            }
        });
        
        mActionBar.setTitle(mUserLogin);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.about)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(isLoginUserPage ? R.string.user_news_feed : R.string.user_public_activity)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab);
        
        if (isLoginUserPage) {
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
        }
    }
    
    public class UserAdapter extends FragmentStatePagerAdapter {

        public UserAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return tabCount;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            Log.i(Constants.LOG_TAG, ">>>>>>>>>>> getItem " + position);
            if (position == 0) {
                return UserFragment.newInstance(UserActivity.this.mUserLogin,
                        UserActivity.this.mUserName);
            }
            else if (position == 1) {
                return PrivateEventListFragment.newInstance(UserActivity.this.mUserLogin, 
                        UserActivity.this.isLoginUserPage);
            }
            else if (position == 2 && isLoginUserPage) {
                return PublicEventListFragment.newInstance(UserActivity.this.mUserLogin, false);
            }
            else if (position == 3 && isLoginUserPage) {
                Map<String, String> filterData = new HashMap<String, String>();
                filterData.put("filter", "subscribed");
                return RepositoryIssueListFragment.newInstance(filterData);
            }
            else {
                return UserFragment.newInstance(UserActivity.this.mUserLogin,
                        UserActivity.this.mUserName);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            Log.i(Constants.LOG_TAG, ">>>>>>>>>>> destroyItem " + container + ", " + position + ", " + object);
        }
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (isLoginUserPage) {
            int selectedTabIndex = mActionBar.getSelectedNavigationIndex();
            if (selectedTabIndex == 3) {
                SubMenu subMenu1 = menu.addSubMenu("Action Item");
                subMenu1.add(R.string.issues_submitted);
                subMenu1.add(R.string.issues_updated);
                subMenu1.add(R.string.issues_comments);
        
                MenuItem subMenu1Item = subMenu1.getItem();
                subMenu1Item.setIcon(R.drawable.abs__ic_menu_share_holo_dark);
                subMenu1Item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
        return super.onCreateOptionsMenu(menu);
    }
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onConfigurationChanged " + newConfig.orientation);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //invalidateOptionsMenu();
        } 
    }
}
