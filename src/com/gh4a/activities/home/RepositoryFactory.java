package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.MenuItem;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.RepositoryListContainerFragment;

public class RepositoryFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
        R.string.my_repositories
    };

    private String mUserLogin;
    private String mSelectedType;
    private RepositoryListContainerFragment.FilterDrawerHelper mDrawerHelper;
    private RepositoryListContainerFragment mFragment;

    private static final String STATE_KEY_FRAGMENT = "repoFactoryFragment";

    public RepositoryFactory(HomeActivity activity, String userLogin, Bundle savedInstanceState) {
        super(activity);
        mUserLogin = userLogin;
    }

    @Override
    protected int getTitleResId() {
        return R.string.my_repositories;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected int[] getToolDrawerMenuResIds() {
        mDrawerHelper = RepositoryListContainerFragment.FilterDrawerHelper.create(mActivity,
                mUserLogin, Constants.User.TYPE_USER);
        return mDrawerHelper.getMenuResIds();
    }

    @Override
    protected boolean onDrawerItemSelected(MenuItem item) {
        String type = mDrawerHelper.handleSelectionAndGetFilterType(item);
        if (type != null && !TextUtils.equals(type, mSelectedType)) {
            mFragment.setFilterType(type);
            mSelectedType = type;
            mActivity.doInvalidateOptionsMenu();
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
        mFragment.destroyChildren();
    }

    @Override
    protected void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        mFragment = (RepositoryListContainerFragment)
                mActivity.getSupportFragmentManager().getFragment(state, STATE_KEY_FRAGMENT);
    }

    @Override
    protected Fragment getFragment(int position) {
        mFragment = RepositoryListContainerFragment.newInstance(mUserLogin,
                Constants.User.TYPE_USER);
        return mFragment;
    }

    @Override
    protected void onRefreshFragment(Fragment fragment) {
        ((RepositoryListContainerFragment) fragment).refresh();
    }
}
