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
    private ActionBar mActionBar;
    private ThisPageAdapter mAdapter;
    private ViewPager mPager;
    private TitlePageIndicator mIndicator;
    private PublicTimelineFragment mPublicTimeFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        if (!isOnline()) {
            setErrorView();
            return;
        }

        setContentView(R.layout.explore);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        ArrayAdapter<CharSequence> list = ArrayAdapter.createFromResource(
                mActionBar.getThemedContext(), R.array.explore_item,
                R.layout.sherlock_spinner_item);
        list.setDropDownViewResource(R.layout.row_simple);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(list, this);

        setPageIndicator(mActionBar.getSelectedNavigationIndex());
    }

    private void setPageIndicator(int position) {
        mAdapter = new ThisPageAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.invalidate();

        mIndicator = (TitlePageIndicator) findViewById(R.id.indicator);

        if (Gh4Application.THEME != R.style.DefaultTheme) {
            mIndicator.setTextColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedColor(getResources().getColor(R.color.abs__primary_text_holo_light));
            mIndicator.setSelectedBold(true);
        }

        boolean trending = position == 1;
        mIndicator.setVisibility(trending ? View.VISIBLE : View.GONE);
        mIndicator.setViewPager(mPager);

        mIndicator.notifyDataSetChanged();
        mAdapter.notifyDataSetChanged();
    }

    private class ThisPageAdapter extends FragmentStatePagerAdapter {
        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return mActionBar.getSelectedNavigationIndex() == 1 ? 3 : 1;
        }

        @Override
        public Fragment getItem(int position) {
            int mode = mActionBar.getSelectedNavigationIndex();
            if (position == 0) {
                if (mode == 0) {
                    mPublicTimeFragment = PublicTimelineFragment.newInstance();
                    return mPublicTimeFragment;
                } else if (mode == 2) {
                    return BlogListFragment.newInstance();
                } else {
                    return TrendingFragment.newInstance(TrendingFragment.TYPE_DAILY);
                }
            } else if (position == 1) {
                return TrendingFragment.newInstance(TrendingFragment.TYPE_WEEKLY);
            } else {
                return TrendingFragment.newInstance(TrendingFragment.TYPE_MONTHLY);
            }
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
        setPageIndicator(itemPosition);
        return true;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        if (mActionBar.getSelectedNavigationIndex() == 0) {
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