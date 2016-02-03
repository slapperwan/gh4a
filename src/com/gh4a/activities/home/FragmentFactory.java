package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

public abstract class FragmentFactory {
    protected HomeActivity mActivity;

    protected FragmentFactory(HomeActivity activity) {
        mActivity = activity;
    }

    protected abstract int getTitleResId();
    protected abstract int[] getTabTitleResIds();
    protected abstract Fragment getFragment(int position);
    protected abstract void onRefreshFragment(Fragment fragment);

    /* expected format: int[tabCount][2] - 0 is header, 1 is status bar */
    protected int[][] getTabHeaderColors() {
        return null;
    }

    protected int[] getToolDrawerMenuResIds() {
        return null;
    }

    protected boolean onDrawerItemSelected(MenuItem item) {
        return false;
    }

    protected boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    protected boolean onOptionsItemSelected(MenuItem item) {
        return false;
    }

    protected void onSaveInstanceState(Bundle outState) {}

    protected void onStart() {}
}
