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
package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.View;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.BlogListFragment;
import com.gh4a.fragment.PublicTimelineFragment;
import com.gh4a.fragment.TrendingFragment;
import com.gh4a.utils.UiUtils;
import com.viewpagerindicator.TitlePageIndicator;

public class ExploreActivity extends BaseSherlockFragmentActivity implements ActionBar.OnNavigationListener {
    private ViewPager mPager;
    private TitlePageIndicator mIndicator;
    private ThisPageAdapter mAdapter;
    private PublicTimelineFragment mPublicTimeFragment;
    private int mSelectedItem;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        if (!isOnline()) {
            setErrorView();
            return;
        }

        setContentView(R.layout.explore);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(
                actionBar.getThemedContext(), R.array.explore_item,
                R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.row_simple);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(list, this);

        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        if (Gh4Application.THEME != R.style.DefaultTheme) {
            mIndicator.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedBold(true);
        }

        mSelectedItem = actionBar.getSelectedNavigationIndex();
        updatePageIndicator();
    }

    private void updatePageIndicator() {
        boolean trending = mSelectedItem == 1;
        mIndicator.setVisibility(trending ? View.VISIBLE : View.GONE);

        mIndicator.notifyDataSetChanged();
        mAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
        mPager.invalidate();
    }

    private class ThisPageAdapter extends FragmentStatePagerAdapter {
        private SparseArray<String> mTrendingTypePositionMap = new SparseArray<String>();

        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
            mTrendingTypePositionMap.put(0, TrendingFragment.TYPE_DAILY);
            mTrendingTypePositionMap.put(1, TrendingFragment.TYPE_WEEKLY);
            mTrendingTypePositionMap.put(2, TrendingFragment.TYPE_MONTHLY);
        }

        @Override
        public int getCount() {
            return mSelectedItem == 1 ? 3 : 1;
        }

        @Override
        public Fragment getItem(int position) {
            switch (mSelectedItem) {
                case 0:
                    mPublicTimeFragment = PublicTimelineFragment.newInstance();
                    return mPublicTimeFragment;
                case 1:
                    return TrendingFragment.newInstance(mTrendingTypePositionMap.get(position));
                case 2:
                    return BlogListFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return getString(R.string.trend_today);
            } else if (position == 1) {
                return getString(R.string.trend_week);
            } else {
                return getString(R.string.trend_month);
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        mSelectedItem = itemPosition;
        updatePageIndicator();
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (mSelectedItem == 0) {
            menu.add(0, R.id.refresh, 0, getString(R.string.refresh))
                .setIcon(UiUtils.resolveDrawable(this, R.attr.refreshIcon))
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        goToToplevelActivity(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                if (mPublicTimeFragment != null) {
                    mPublicTimeFragment.refresh();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}