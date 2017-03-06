package com.gh4a.activities.home;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.GistListFragment;

public class GistFactory extends FragmentFactory {
    private final String mUserLogin;

    private static final int[] TAB_TITLES = new int[] {
        R.string.mine, R.string.starred
    };

    public GistFactory(HomeActivity activity, String userLogin) {
        super(activity);
        mUserLogin = userLogin;
    }

    @Override
    protected @StringRes int getTitleResId() {
        return R.string.my_gists;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return GistListFragment.newInstance(mUserLogin, position == 1);
    }
}
