package com.gh4a.activities.home;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

public abstract class FragmentFactory {
    protected final HomeActivity mActivity;

    protected FragmentFactory(HomeActivity activity) {
        mActivity = activity;
    }

    protected abstract @StringRes int getTitleResId();
    protected abstract int[] getTabTitleResIds();
    protected abstract Fragment getFragment(int position);

    protected int[] getHeaderColors() {
        return null;
    }

    protected int[] getToolDrawerMenuResIds() {
        return null;
    }

    protected void prepareToolDrawerMenu(Menu menu) {

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

    protected void onRestoreInstanceState(Bundle state) {}

    protected void onDestroy() {}

    protected @IdRes int getInitialToolDrawerSelection() {
        return 0;
    }
}
