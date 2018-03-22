package com.gh4a.activities.home;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.NotificationListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;

public class NotificationListFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
            R.string.notifications, R.string.watching
    };

    private final String mUserLogin;

    protected NotificationListFactory(HomeActivity activity, String userLogin) {
        super(activity);
        mUserLogin = userLogin;
    }

    @Override
    @StringRes
    protected int getTitleResId() {
        return R.string.notifications;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        if (position == 1) {
            return WatchedRepositoryListFragment.newInstance(mUserLogin);
        }
        return NotificationListFragment.newInstance();
    }
}
