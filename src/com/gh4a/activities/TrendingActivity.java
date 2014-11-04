package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.BasePagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.TrendingFragment;

public class TrendingActivity extends BasePagerActivity {
    private static final int[] TITLES = new int[] {
        R.string.trend_today, R.string.trend_week, R.string.trend_month
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.trend);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
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
