package com.gh4a.activities.home;

import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.PublicTimelineFragment;

public class TimelineFactory extends FragmentFactory {
    private static final int[] TAB_TITLES = new int[] {
        R.string.pub_timeline
    };

    public TimelineFactory(HomeActivity activity) {
        super(activity);
    }

    @Override
    protected int getTitleResId() {
        return R.string.pub_timeline;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        return PublicTimelineFragment.newInstance();
    }
}
