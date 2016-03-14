package com.gh4a.activities.home;

import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.PrivateEventListFragment;

public class NewsFeedFactory extends FragmentFactory {
    private final String mUserLogin;

    private static final int[] TAB_TITLES = new int[] {
        R.string.user_news_feed
    };

    public NewsFeedFactory(HomeActivity activity, String userLogin) {
        super(activity);
        mUserLogin = userLogin;
    }

    @Override
    public int getTitleResId() {
        return R.string.user_news_feed;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        return PrivateEventListFragment.newInstance(mUserLogin);
    }
}
