package com.gh4a;

import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ViewGroup;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;
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
    private UserFragment mUserFragment;
    private PrivateEventListFragment mPrivateEventListFragment;
    private PublicEventListFragment mPublicEventListFragment;
    private RepositoryIssueListFragment mRepositoryIssueListFragment;
    public boolean isFinishLoadingFollowing;
    public boolean isFollowing;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserName = data.getString(Constants.User.USER_NAME);
        int position = data.getInt("position");
        
        if (!isOnline()) {
            setErrorView();
            return;
        }

        setContentView(R.layout.view_pager);

        BugSenseHandler.setup(this, "6e1b031");
        
        isLoginUserPage = mUserLogin.equals(getAuthLogin());
        
        if (isLoginUserPage) {
            tabCount = 4;
        }
        else {
            tabCount = 2;
        }
        
        mActionBar = getSupportActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (isLoginUserPage) {
            mActionBar.setDisplayShowHomeEnabled(false);
            mActionBar.setDisplayShowTitleEnabled(false);
        }
        else {
            mActionBar.setTitle(mUserLogin);
            mActionBar.setDisplayHomeAsUpEnabled(true);            
        }
        
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
            }
        });
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.about)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, position == 0);
        
        tab = mActionBar
                .newTab()
                .setText(isLoginUserPage ? R.string.user_news_feed : R.string.user_public_activity)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, position == 1);
        
        if (isLoginUserPage) {
            tab = mActionBar
                    .newTab()
                    .setText(R.string.user_your_actions)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
            mActionBar.addTab(tab, position == 2);
            
            tab = mActionBar
                    .newTab()
                    .setText(R.string.issues)
                    .setTabListener(
                            new TabListener<SherlockFragmentActivity>(this, 3 + "", mPager));
            mActionBar.addTab(tab, position == 3);
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
                mUserFragment = UserFragment.newInstance(UserActivity.this.mUserLogin,
                        UserActivity.this.mUserName);
                return mUserFragment;
            }
            else if (position == 1) {
                mPrivateEventListFragment = (PrivateEventListFragment) PrivateEventListFragment
                        .newInstance(UserActivity.this.mUserLogin, 
                        UserActivity.this.isLoginUserPage);
                return mPrivateEventListFragment;
            }
            else if (position == 2 && isLoginUserPage) {
                mPublicEventListFragment = (PublicEventListFragment) PublicEventListFragment
                        .newInstance(UserActivity.this.mUserLogin, false);
                return mPublicEventListFragment;
            }
            else if (position == 3 && isLoginUserPage) {
                Map<String, String> filterData = new HashMap<String, String>();
                filterData.put("filter", "subscribed");
                mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
                return mRepositoryIssueListFragment;
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
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onConfigurationChanged " + newConfig.orientation);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //invalidateOptionsMenu();
        } 
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        
        MenuItem followAction = menu.getItem(1);
        
        if (mUserLogin.equals(getAuthLogin())) {
            menu.getItem(2).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.removeItem(R.id.follow);
        }
        else {
            if (!isFinishLoadingFollowing) {
                followAction.setActionView(R.layout.ab_loading);
                followAction.expandActionView();
            }
            else {
                if (isFollowing) {
                    followAction.setTitle(R.string.user_unfollow_action);
                }
                else {
                    followAction.setTitle(R.string.user_follow_action);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openUserInfoActivity(this, getAuthLogin(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                int currentTab = mPager.getCurrentItem();
                if (currentTab == 0 && mUserFragment != null) {
                    mUserFragment.refresh();
                } 
                else if (currentTab == 1 && mPrivateEventListFragment != null) {
                    mPrivateEventListFragment.refresh();
                }
                else if (currentTab == 2 && mPublicEventListFragment != null) {
                    mPublicEventListFragment.refresh();
                }
                else {
                    if (mRepositoryIssueListFragment != null) {
                        mRepositoryIssueListFragment.refresh();
                    }
                }
                return true;
            case R.id.logout:
                SharedPreferences sharedPreferences = getApplication().getSharedPreferences(
                        Constants.PREF_NAME, MODE_PRIVATE);
                
                if (sharedPreferences != null) {
                    if (sharedPreferences.getString(Constants.User.USER_LOGIN, null) != null
                            && sharedPreferences.getString(Constants.User.USER_AUTH_TOKEN, null) != null){
                        Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.commit();
                        Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                                |Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        this.finish();
                    }
                }
                return true;
            case R.id.dark:
                Gh4Application.THEME = R.style.DefaultTheme;
                saveTheme(R.style.DefaultTheme);
                return true;
            case R.id.light:
                Gh4Application.THEME = R.style.LightTheme;
                saveTheme(R.style.LightTheme);
                return true;
            case R.id.lightDark:
                Gh4Application.THEME = R.style.LightDarkTheme;
                saveTheme(R.style.LightDarkTheme);
                return true;
            case R.id.pub_timeline:
                Intent intent = new Intent().setClass(this, ExploreActivity.class);
                intent.putExtra("exploreItem", 0);
                startActivity(intent);
                return true;
            case R.id.trend:
                intent = new Intent().setClass(this, ExploreActivity.class);
                intent.putExtra("exploreItem", 1);
                startActivity(intent);
                return true;
            case R.id.blog:
                intent = new Intent().setClass(this, ExploreActivity.class);
                intent.putExtra("exploreItem", 2);
                startActivity(intent);
                return true;
            case R.id.follow:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                mUserFragment.followUser(mUserLogin);
                return true;
            default:
                return true;
        }
    }
    
    private void saveTheme(int theme) {
        SharedPreferences sharedPreferences = getSharedPreferences(
                Constants.PREF_NAME, MODE_PRIVATE);
        Editor editor = sharedPreferences.edit();
        editor.putInt("THEME", theme);
        editor.commit();
        
        recreate();
        
    }
    
    /**
     * Copy from 
     * https://github.com/JakeWharton/ActionBarSherlock/blob/bd8d05b6a0302dba40a12cdfc4c3f0f77b4a9e54/library/src/android/support/v4/app/FragmentActivity.java#L926-961
     */
    @Override
    public void recreate() {
        //This SUCKS! Figure out a way to call the super method and support Android 1.6
        /*
        if (IS_HONEYCOMB) {
            super.recreate();
        } else {
        */
            final Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                OverridePendingTransition.invoke(this);
            }

            finish();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
                OverridePendingTransition.invoke(this);
            }
        /*
        }
        */
    }
    
    private static final class OverridePendingTransition {
        static void invoke(Activity activity) {
            activity.overridePendingTransition(0, 0);
        }
    }
    
    public void updateFollowingAction(boolean isFollowing) {
        this.isFollowing = isFollowing;
        this.isFinishLoadingFollowing = true;
        invalidateOptionsMenu();
    }
}
