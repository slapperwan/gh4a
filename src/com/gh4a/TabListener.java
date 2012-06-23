package com.gh4a;

import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class TabListener<T extends SherlockFragmentActivity> implements ActionBar.TabListener {
    
    private SherlockFragmentActivity mActivity;
    private final String mTag;
    private ViewPager mPager;

    public TabListener(SherlockFragmentActivity activity, String tag, ViewPager pager) {
        mActivity = activity;
        mTag = tag;
        mPager = pager;
    }

    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onTabSelected " + tab + ", " + mActivity.getSupportActionBar().getSelectedNavigationIndex());
        //mPager.setCurrentItem(Integer.parseInt(mTag));
        mPager.setCurrentItem(mActivity.getSupportActionBar().getSelectedNavigationIndex());
    }

    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onTabUnselected " + tab);
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        Log.i(Constants.LOG_TAG, ">>>>>>>>>>> onTabReselected " + tab);
    }
}
