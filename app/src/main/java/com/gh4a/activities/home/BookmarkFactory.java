package com.gh4a.activities.home;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.BlogListFragment;
import com.gh4a.fragment.BookmarkListFragment;

public class BookmarkFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
            R.string.bookmarks
    };

    public BookmarkFactory(HomeActivity activity) {
        super(activity);
    }

    @Override
    protected @StringRes int getTitleResId() {
        return R.string.bookmarks;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return BookmarkListFragment.newInstance();
    }
}
