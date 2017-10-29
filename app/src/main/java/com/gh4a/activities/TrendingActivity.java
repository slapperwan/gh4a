package com.gh4a.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.TrendingFragment;

public class TrendingActivity extends BaseFragmentPagerActivity {
    private static final int[] TITLES = new int[] {
        R.string.trend_today, R.string.trend_week, R.string.trend_month
    };

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.trend);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        switch (position) {
            case 0: return TrendingFragment.newInstance(TrendingFragment.TYPE_DAILY);
            case 1: return TrendingFragment.newInstance(TrendingFragment.TYPE_WEEKLY);
            case 2: return TrendingFragment.newInstance(TrendingFragment.TYPE_MONTHLY);
        }
        return null;
    }

    @Override
    protected Intent navigateUp() {
        return getToplevelActivityIntent();
    }
}
