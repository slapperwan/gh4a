package com.gh4a;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

public abstract class BasePagerActivity extends BaseActivity implements
        SwipeRefreshLayout.ChildScrollDelegate, ViewPager.OnPageChangeListener {
    private FragmentAdapter mAdapter;
    private TabLayout mTabs;
    private ViewPager mPager;
    private int[][] mTabHeaderColors;
    private boolean mScrolling;
    private boolean mErrorViewVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new FragmentAdapter();
        setContentView(R.layout.view_pager);

        mTabHeaderColors = getTabHeaderColors();
        mPager = setupPager();
        updateTabVisibility();

        setChildScrollDelegate(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        onPageMoved(0, 0);
    }

    protected void invalidateFragments() {
        mAdapter.notifyDataSetChanged();
    }

    protected void invalidateTabs() {
        invalidateFragments();
        mTabHeaderColors = getTabHeaderColors();
        if (mTabHeaderColors != null) {
            onPageMoved(0, 0);
        } else {
            transitionHeaderToColor(R.attr.colorPrimary, R.attr.colorPrimaryDark);
        }
        mTabs.setupWithViewPager(mPager);
        updateTabVisibility();
    }

    protected ViewPager getPager() {
        return mPager;
    }

    protected void setTabsEnabled(boolean enabled) {
        mPager.setEnabled(enabled);
    }

    @Override
    protected void setErrorViewVisibility(boolean visible) {
        mErrorViewVisible = visible;
        updateTabVisibility();
        super.setErrorViewVisibility(visible);
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
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(mAdapter);
        pager.addOnPageChangeListener(this);

        mTabs = (TabLayout) getLayoutInflater().inflate(R.layout.tab_bar, null);
        mTabs.setupWithViewPager(pager);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            toolbar.addView(mTabs, new Toolbar.LayoutParams(
                    Toolbar.LayoutParams.WRAP_CONTENT,
                    Toolbar.LayoutParams.MATCH_PARENT));
        } else {
            addHeaderView(mTabs, false);
        }

        return pager;
    }

    private void updateTabVisibility() {
        int[] titleResIds = getTabTitleResIds();

        // We never have many pages, make sure to keep them all alive
        mPager.setOffscreenPageLimit(titleResIds.length - 1);

        mTabs.setVisibility(titleResIds.length > 1 && !mErrorViewVisible ? View.VISIBLE : View.GONE);
        setToolbarScrollable(titleResIds.length > 1
                && getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE);

        LinearLayout tabStrip = (LinearLayout) mTabs.getChildAt(0);
        for (int i = 0; i < tabStrip.getChildCount(); i++) {
            View tab = tabStrip.getChildAt(i);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tab.getLayoutParams();
            lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
            lp.weight = 1;
            tab.setLayoutParams(lp);
        }
    }

    protected abstract int[] getTabTitleResIds();
    protected abstract Fragment getFragment(int position);
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return false;
    }

    /* expected format: int[tabCount][2] - 0 is header, 1 is status bar */
    protected int[][] getTabHeaderColors() {
        return null;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        onPageMoved(position, positionOffset);
    }

    @Override
    public void onPageSelected(int position) {
        if (!mScrolling) {
            onPageMoved(position, 0);
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        mScrolling = state != ViewPager.SCROLL_STATE_IDLE;
    }

    protected void onPageMoved(int position, float fraction) {
        if (mTabHeaderColors != null) {
            int nextIndex = Math.max(position, mTabHeaderColors.length - 1);
            int headerColor = UiUtils.mixColors(mTabHeaderColors[position][0],
                    mTabHeaderColors[nextIndex][0], fraction);
            int statusBarColor = UiUtils.mixColors(mTabHeaderColors[position][1],
                    mTabHeaderColors[nextIndex][1], fraction);
            setHeaderColor(headerColor, statusBarColor);
        }
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