package com.gh4a;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gh4a.widget.SlidingTabLayout;
import com.gh4a.widget.SwipeRefreshLayout;

public abstract class BasePagerActivity extends BaseActivity implements
        SwipeRefreshLayout.ChildScrollDelegate {
    private FragmentAdapter mAdapter;
    private ViewPager mPager;
    private SlidingTabLayout mTabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        mAdapter = new FragmentAdapter();
        setContentView(R.layout.view_pager);

        mPager = setupPager();

        setChildScrollDelegate(this);
    }

    protected void invalidateFragments() {
        mAdapter.notifyDataSetChanged();
    }

    protected ViewPager getPager() {
        return mPager;
    }

    protected void setTabsEnabled(boolean enabled) {
        mPager.setEnabled(enabled);
    }

    @Override
    public boolean canChildScrollUp() {
        Fragment item = mAdapter.getCurrentFragment();
        if (item != null && item instanceof SwipeRefreshLayout.ChildScrollDelegate) {
            return ((SwipeRefreshLayout.ChildScrollDelegate) item).canChildScrollUp();
        }
        return false;
    }

    private ViewPager setupPager() {
        int[] titleResIds = getTabTitleResIds();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(mAdapter);

        // We never have many pages, make sure to keep them all alive
        pager.setOffscreenPageLimit(titleResIds.length - 1);

        mTabs = new SlidingTabLayout(this);
        mTabs.setSelectedIndicatorColors(getResources().getColor(R.color.tab_indicator_color));
        mTabs.setCustomTabView(R.layout.tab_indicator, R.id.tab_title);
        mTabs.setDistributeEvenly(true);
        mTabs.setViewPager(pager);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
            toolBar.addView(mTabs, new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.MATCH_PARENT));
        } else {
            LinearLayout header = (LinearLayout) findViewById(R.id.header);
            header.addView(mTabs, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        return pager;
    }

    protected abstract int[] getTabTitleResIds();
    protected abstract Fragment getFragment(int position);
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return false;
    }

    private class FragmentAdapter extends FragmentStatePagerAdapter {
        private Fragment mCurrentFragment;

        public FragmentAdapter() {
            super(getSupportFragmentManager());
        }

        @Override
        public int getCount() {
            return getTabTitleResIds().length;
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        private Fragment getCurrentFragment() {
            return mCurrentFragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(getTabTitleResIds()[position]);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            if (object == mCurrentFragment) {
                mCurrentFragment = null;
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            mCurrentFragment = (Fragment) object;
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public int getItemPosition(Object object) {
            if (object instanceof Fragment && fragmentNeedsRefresh((Fragment) object)) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }
}