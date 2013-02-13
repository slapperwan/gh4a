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
import com.gh4a.fragment.TrendingFragment;

public class TrendingActivity extends BaseSherlockFragmentActivity {

    private static final String TODAY = "http://github-trends.oscardelben.com/explore/today.xml";
    private static final String WEEK = "http://github-trends.oscardelben.com/explore/week.xml";
    private static final String MONTH = "http://github-trends.oscardelben.com/explore/month.xml";
    private static final String FOREVER = "http://github-trends.oscardelben.com/explore/forever.xml";
    
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private int tabCount;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        tabCount = 4;
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.trend);
        actionBar.setSubtitle(R.string.explore);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(true);
        
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
                actionBar.getTabAt(position).select();
            }
        });
        
        Tab tab = actionBar
                .newTab()
                .setText(R.string.trend_today)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        actionBar.addTab(tab);
        
        tab = actionBar
                .newTab()
                .setText(R.string.trend_week)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        actionBar.addTab(tab);
        
        tab = actionBar
                .newTab()
                .setText(R.string.trend_month)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        actionBar.addTab(tab);
        
        tab = actionBar
                .newTab()
                .setText(R.string.trend_forever)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 3 + "", mPager));
        actionBar.addTab(tab);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.explore_menu, menu);
        menu.removeItem(R.id.refresh);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!isAuthorized()) {
                    Intent intent = new Intent().setClass(this, Github4AndroidActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
                else {
                    getApplicationContext().openUserInfoActivity(this, getAuthLogin(), 
                            null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    return true;
                }
            case R.id.pub_timeline:
                Intent intent = new Intent().setClass(this, TimelineActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.trend:
                intent = new Intent().setClass(this, TrendingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.blog:
                intent = new Intent().setClass(this, BlogListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            default:
                return true;
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
                return TrendingFragment.newInstance(TODAY);
            }
            else if (position == 1) {
                return TrendingFragment.newInstance(WEEK);
            }
            else if (position == 2) {
                return TrendingFragment.newInstance(MONTH);
            }
            else if (position == 3) {
                return TrendingFragment.newInstance(FOREVER);
            }
            else {
                return TrendingFragment.newInstance(TODAY);
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
        }
    }
}
