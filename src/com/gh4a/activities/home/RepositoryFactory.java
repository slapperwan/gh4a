package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.widget.ListAdapter;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.RepositoryListContainerFragment;

public class RepositoryFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
        R.string.my_repositories
    };

    private String mUserLogin;
    private int mSelectedIndex;
    private String mSelectedType;
    private RepositoryListContainerFragment.FilterDrawerAdapter mDrawerAdapter;
    private RepositoryListContainerFragment mFragment;

    private static final String STATE_KEY_SELECTED_INDEX = "selectedRepoFilterIndex";

    public RepositoryFactory(HomeActivity activity, String userLogin, Bundle savedInstanceState) {
        super(activity);
        mUserLogin = userLogin;
        mSelectedIndex = savedInstanceState != null
                ? savedInstanceState.getInt(STATE_KEY_SELECTED_INDEX) : 0;
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
    protected ListAdapter getToolDrawerAdapter() {
        mDrawerAdapter = RepositoryListContainerFragment.FilterDrawerAdapter.create(
                mActivity, mUserLogin, Constants.User.TYPE_USER);
        return mDrawerAdapter;
    }

    @Override
    protected boolean onDrawerItemSelected(int position) {
        String type = mDrawerAdapter.handleSelectionAndGetFilterType(position);
        if (type != null && !TextUtils.equals(type, mSelectedType)) {
            mFragment.setFilterType(type);
            mSelectedIndex = position;
            mSelectedType = type;
            mDrawerAdapter.notifyDataSetChanged();
            mActivity.doInvalidateOptionsMenu();
            return true;
        }
        return super.onDrawerItemSelected(position);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY_SELECTED_INDEX, mSelectedIndex);
    }

    @Override
    protected void onStart() {
        super.onStart();
        onDrawerItemSelected(mSelectedIndex);
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
