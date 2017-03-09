package com.gh4a.activities.home;

import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.NotificationListFragment;

public class NotificationListFactory extends FragmentFactory {
    private static final int[] TAB_TITLES =  new int[] {
        R.string.notifications
    };

    protected NotificationListFactory(HomeActivity activity) {
        super(activity);
    }

    @Override
    protected @StringRes int getTitleResId() {
        return R.string.notifications;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        return NotificationListFragment.newInstance();
    }
}
