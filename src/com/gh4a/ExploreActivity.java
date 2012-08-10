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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.BlogListFragment;
import com.gh4a.fragment.PublicTimelineFragment;
import com.gh4a.fragment.TrendingFragment;
import com.viewpagerindicator.TitlePageIndicator;

public class ExploreActivity extends BaseSherlockFragmentActivity implements ActionBar.OnNavigationListener {
    
    private static final String TODAY = "http://github-trends.oscardelben.com/explore/today.xml";
    private static final String WEEK = "http://github-trends.oscardelben.com/explore/week.xml";
    private static final String MONTH = "http://github-trends.oscardelben.com/explore/month.xml";
    private static final String FOREVER = "http://github-trends.oscardelben.com/explore/forever.xml";
    
    private ActionBar mActionBar;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private int tabCount;
    private TitlePageIndicator mIndicator;
    private int mExploreItem;
    private PublicTimelineFragment mPublicTimeFragment;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mExploreItem = getIntent().getExtras().getInt("exploreItem");
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.explore);
        
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        Context context = getSupportActionBar().getThemedContext();
        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(context, R.array.explore_item, R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.row_simple);
        
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getSupportActionBar().setListNavigationCallbacks(list, this);
        getSupportActionBar().setSelectedNavigationItem(mExploreItem);
        
        setPageIndicator();
    }
    
    private void setPageIndicator() {
        if (mExploreItem == 1) {//trending
            tabCount = 4;
        }
        else {
            tabCount = 1;
        }
        
        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.invalidate();
        
        mIndicator = (TitlePageIndicator)findViewById(R.id.indicator);
        
        if (Gh4Application.THEME != R.style.DefaultTheme) {
            mIndicator.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedBold(true);
        }
        
        if (mExploreItem == 1) {//trending
            mIndicator.setVisibility(View.VISIBLE);
        }
        else {
            mIndicator.setVisibility(View.GONE);
        }
        mIndicator.setViewPager(mPager);
        
        mIndicator.notifyDataSetChanged();
        mAdapter.notifyDataSetChanged();
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
                if (mExploreItem == 0) {
                    mPublicTimeFragment = PublicTimelineFragment.newInstance();
                    return mPublicTimeFragment;
                }
                else if (mExploreItem == 2) {
                    return BlogListFragment.newInstance();
                }
                else {
                    return TrendingFragment.newInstance(TODAY);
                }
            }
            else if (position == 1) {
                return TrendingFragment.newInstance(WEEK);
            }
            else if (position == 2) {
                return TrendingFragment.newInstance(MONTH);
            }
            else {
                return TrendingFragment.newInstance(FOREVER);
            }
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getResources().getString(R.string.trend_today);
            }
            else if (position == 1) {
                return getResources().getString(R.string.trend_week);
            }
            else if (position == 2) {
                return getResources().getString(R.string.trend_month);
            }
            else {
                return getResources().getString(R.string.trend_forever);
            }
        }
        
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition == 0) {
            mExploreItem = 0;
        }
        else if (itemPosition == 1) {
            mExploreItem = 1;
        }
        else if (itemPosition == 2) {
            mExploreItem = 2;
        }
        
        setPageIndicator();
        
        return true;
    }
    
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (mExploreItem == 0) {
            menu.add(0, R.id.refresh, 0, "Refresh")
                .setIcon(Gh4Application.THEME != R.style.LightTheme ? 
                        R.drawable.navigation_refresh_dark : R.drawable.navigation_refresh)
                .setShowAsAction(com.actionbarsherlock.view.MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onPrepareOptionsMenu(menu);
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
                    getApplicationContext().openUserInfoActivity(this, getAuthLogin(), null);
                    return true;
                }
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                if (mPublicTimeFragment != null) {
                    mPublicTimeFragment.refresh();
                } 
                return true;
            default:
                return true;
        }
    }
}
