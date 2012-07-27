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
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.USER_LOGIN);
        mUserName = data.getString(Constants.User.USER_NAME);
        int position = data.getInt("position");
        
        isLoginUserPage = mUserLogin.equals(getAuthLogin());
        
        if (isLoginUserPage) {
            tabCount = 4;
        }
        else {
            tabCount = 2;
        }
        
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mUserLogin);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        if (mUserLogin.equals(getAuthLogin())) {
            mActionBar.setHomeButtonEnabled(false);            
        }
        else {
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
                invalidateOptionsMenu();
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
    
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onConfigurationChanged " + newConfig.orientation);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
                || newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //invalidateOptionsMenu();
        } 
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openUserInfoActivity(this, getAuthLogin(), null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.refresh:
                Intent intent = new Intent().setClass(this, UserActivity.class);
                intent.putExtra(Constants.User.USER_LOGIN, mUserLogin);
                intent.putExtra(Constants.User.USER_NAME, mUserName);
                intent.putExtra("position", mPager.getCurrentItem());
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.theme:
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
                        intent = new Intent().setClass(this, Github4AndroidActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP
                                |Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        this.finish();
                    }
                }
                return true;
            case R.id.dark:
                Gh4Application.THEME = R.style.DarkTheme;
                saveTheme(R.style.DarkTheme);
                return true;
            case R.id.light:
                Gh4Application.THEME = R.style.LightTheme;
                saveTheme(R.style.LightTheme);
                return true;
            case R.id.lightDark:
                Gh4Application.THEME = R.style.DefaultTheme;
                saveTheme(R.style.DefaultTheme);
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

}
