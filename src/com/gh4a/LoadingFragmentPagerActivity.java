package com.gh4a;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;

public abstract class LoadingFragmentPagerActivity extends LoadingFragmentActivity {
    private FragmentAdapter mAdapter;
    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        mAdapter = new FragmentAdapter();
        setContentView(R.layout.view_pager);
        mPager = setupPager();
    }

    protected void invalidateFragments() {
        mAdapter.notifyDataSetChanged();
    }

    protected ViewPager getPager() {
        return mPager;
    }

    public void setTabsEnabled(boolean enabled) {
        mPager.setEnabled(enabled);
        if (enabled) {
            mPager.setCurrentItem(getSupportActionBar().getSelectedTab().getPosition());
        }
    }

    private ViewPager setupPager() {
        final ActionBar actionBar = getSupportActionBar();
        int[] titleResIds = getTabTitleResIds();
        ViewPager pager = (ViewPager) findViewById(R.id.pager);
        pager.setAdapter(mAdapter);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        pager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {}

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

            @Override
            public void onPageSelected(int position) {
                actionBar.getTabAt(position).select();
            }
        });

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (int i = 0; i < titleResIds.length; i++) {
            actionBar.addTab(actionBar.newTab()
                .setText(titleResIds[i])
                .setTabListener(new TabListener(i, pager)));
        }

        return pager;
    }

    protected abstract int[] getTabTitleResIds();
    protected abstract Fragment getFragment(int position);
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return false;
    }

    private class FragmentAdapter extends FragmentStatePagerAdapter {
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
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (fragmentNeedsRefresh((Fragment) object)) {
                super.destroyItem(container, position, object);
            }
        }
        
        @Override
        public int getItemPosition(Object object) {
            if (object instanceof Fragment && fragmentNeedsRefresh((Fragment) object)) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    private static class TabListener implements ActionBar.TabListener {
        private final int mTag;
        private ViewPager mPager;

        public TabListener(int tag, ViewPager pager) {
            mTag = tag;
            mPager = pager;
        }

        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (mPager.isEnabled()) {
                mPager.setCurrentItem(mTag);
            }
        }

        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        }

        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }
    }
}