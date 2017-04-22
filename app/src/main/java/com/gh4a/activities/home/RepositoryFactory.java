package com.gh4a.activities.home;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

import com.gh4a.R;
import com.gh4a.fragment.RepositoryListContainerFragment;

public class RepositoryFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
        R.string.my_repositories
    };

    private final String mUserLogin;
    private final RepositoryListContainerFragment.FilterDrawerHelper mFilterDrawerHelper;
    private final RepositoryListContainerFragment.SortDrawerHelper mSortDrawerHelper;
    private RepositoryListContainerFragment mFragment;
    private final SharedPreferences mPrefs;

    private static final String STATE_KEY_FRAGMENT = "repoFactoryFragment";
    private static final String PREF_KEY_FILTER = "home_repo_list_filter";
    private static final String PREF_KEY_SORT_ORDER = "home_repo_list_sort_order";
    private static final String PREF_KEY_SORT_DIR = "home_repo_list_sort_dir";

    public RepositoryFactory(HomeActivity activity, String userLogin, SharedPreferences prefs) {
        super(activity);
        mUserLogin = userLogin;

        mFilterDrawerHelper = RepositoryListContainerFragment.FilterDrawerHelper.create(mUserLogin, false);
        mSortDrawerHelper = new RepositoryListContainerFragment.SortDrawerHelper();
        mPrefs = prefs;
    }

    @Override
    protected @StringRes int getTitleResId() {
        return R.string.my_repositories;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected int[] getToolDrawerMenuResIds() {
        int sortMenuResId = mSortDrawerHelper.getMenuResId();
        int filterMenuResId = mFilterDrawerHelper.getMenuResId();
        if (sortMenuResId == 0) {
            return new int[] { filterMenuResId };
        } else {
            return new int[] { sortMenuResId, filterMenuResId };
        }
    }

    @Override
    protected void prepareToolDrawerMenu(Menu menu) {
        super.prepareToolDrawerMenu(menu);
        if (mFragment != null) {
            mFilterDrawerHelper.selectFilterType(menu, mFragment.getFilterType());
            mSortDrawerHelper.selectSortType(menu,
                    mFragment.getSortOrder(), mFragment.getSortDirection());
        }
    }

    @Override
    protected boolean onDrawerItemSelected(MenuItem item) {
        String type = mFilterDrawerHelper.handleSelectionAndGetFilterType(item);
        if (type != null) {
            mFragment.setFilterType(type);
            mSortDrawerHelper.setFilterType(type);
            mPrefs.edit().putString(PREF_KEY_FILTER, type).apply();
            mActivity.doInvalidateOptionsMenuAndToolDrawer();
            return true;
        }
        String[] sortOrderAndDirection = mSortDrawerHelper.handleSelectionAndGetSortOrder(item);
        if (sortOrderAndDirection != null) {
            mFragment.setSortOrder(sortOrderAndDirection[0], sortOrderAndDirection[1]);
            mPrefs.edit()
                    .putString(PREF_KEY_SORT_ORDER, sortOrderAndDirection[0])
                    .putString(PREF_KEY_SORT_DIR, sortOrderAndDirection[1])
                    .apply();
            mActivity.doInvalidateOptionsMenuAndToolDrawer();
            return true;
        }
        return super.onDrawerItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mFragment != null) {
            mActivity.getSupportFragmentManager().putFragment(outState, STATE_KEY_FRAGMENT, mFragment);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFragment != null) {
            mFragment.destroyChildren();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mFragment = (RepositoryListContainerFragment)
                mActivity.getSupportFragmentManager().getFragment(state, STATE_KEY_FRAGMENT);
        if (mFragment != null) {
            mSortDrawerHelper.setFilterType(mFragment.getFilterType());
            restorePreviouslySelectedFilterAndSort();
        }
    }

    @Override
    protected Fragment makeFragment(int position) {
        return RepositoryListContainerFragment.newInstance(mUserLogin, false);
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        mFragment = (RepositoryListContainerFragment) f;
        restorePreviouslySelectedFilterAndSort();
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mFragment) {
            mFragment.destroyChildren();
            mFragment = null;
        }
    }

    private void restorePreviouslySelectedFilterAndSort() {
        String lastType = mPrefs.getString(PREF_KEY_FILTER, null);
        String lastOrder = mPrefs.getString(PREF_KEY_SORT_ORDER, null);
        String lastDir = mPrefs.getString(PREF_KEY_SORT_DIR, null);
        if (lastType != null) {
            mFragment.setFilterType(lastType);
            mSortDrawerHelper.setFilterType(lastType);
        }
        if (lastOrder != null && lastDir != null) {
            mFragment.setSortOrder(lastOrder, lastDir);
        }
        mActivity.doInvalidateOptionsMenuAndToolDrawer();
    }
}
