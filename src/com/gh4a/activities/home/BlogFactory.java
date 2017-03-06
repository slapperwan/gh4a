package com.gh4a.activities.home;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.BlogListFragment;

public class BlogFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
        R.string.blog
    };

    public BlogFactory(HomeActivity activity) {
        super(activity);
    }

    @Override
    protected @StringRes int getTitleResId() {
        return R.string.blog;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return BlogListFragment.newInstance();
    }
}
