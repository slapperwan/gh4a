package com.gh4a.activities.home;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.BookmarkListFragment;
import com.gh4a.fragment.StarredRepositoryListFragment;

public class BookmarkFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
            R.string.bookmarks, R.string.starred
    };

    private static final String PREF_KEY_SORT_ORDER = "home_starred_list_sort_order";
    private static final String PREF_KEY_SORT_DIR = "home_starred_list_sort_dir";

    private final String mUserLogin;
    private StarredRepositoryListFragment mStarredRepoFragment;
    private SharedPreferences mPrefs;

    public BookmarkFactory(HomeActivity activity, String userLogin, SharedPreferences prefs) {
        super(activity);
        mUserLogin = userLogin;
        mPrefs = prefs;
    }

    @Override
    @StringRes
    protected int getTitleResId() {
        return R.string.bookmarks_and_stars;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        if (position == 1) {
            return StarredRepositoryListFragment.newInstance(mUserLogin);
        }
        return BookmarkListFragment.newInstance();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveLastSortOrder();
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        if (position == 1) {
            mStarredRepoFragment = (StarredRepositoryListFragment) f;
            loadLastSortOrder();
        }
        super.onFragmentInstantiated(f, position);
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mStarredRepoFragment) {
            saveLastSortOrder();
            mStarredRepoFragment = null;
        }
        super.onFragmentDestroyed(f);
    }

    private void loadLastSortOrder() {
        String order = mPrefs.getString(PREF_KEY_SORT_ORDER, null);
        String dir = mPrefs.getString(PREF_KEY_SORT_DIR, null);
        if (order != null && dir != null) {
            mStarredRepoFragment.setSortOrderAndDirection(order, dir);
        }
    }

    private void saveLastSortOrder() {
        if (mStarredRepoFragment == null) {
            return;
        }
        mPrefs.edit()
                .putString(PREF_KEY_SORT_ORDER, mStarredRepoFragment.getSortOrder())
                .putString(PREF_KEY_SORT_DIR, mStarredRepoFragment.getSortDirection())
                .apply();
    }
}
