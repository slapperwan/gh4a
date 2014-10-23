package com.gh4a;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.activities.BaseFragmentActivity;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class LoadingFragmentActivity extends BaseFragmentActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    private View mContentContainer;
    private View mContentView;
    private View mEmptyView;
    private boolean mContentShown;
    private SmoothProgressBar mProgress;
    protected SwipeRefreshLayout mSwipeLayout;
    protected DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isOnline()) {
            super.setContentView(R.layout.loading_activity);
            setupSwipeToRefresh();
            setupNavigationDrawer();
        } else {
            setErrorView();
        }
    }

    @Override
    protected void onStart() {
        if (!hasErrorView()) {
            ensureContent();
            if (mContentView == null) {
                throw new IllegalStateException("Content view must be initialized before");
            }
        }
        super.onStart();
    }

    protected boolean canSwipeToRefresh() {
        return false;
    }

    @Override
    public void onRefresh() {
        mSwipeLayout.setRefreshing(true);
    }

    protected void refreshDone() {
        mSwipeLayout.setRefreshing(false);
    }

    private void setupSwipeToRefresh() {
        mSwipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        if (canSwipeToRefresh()) {
            mSwipeLayout.setOnRefreshListener(this);
            mSwipeLayout.setColorSchemeColors(
                    UiUtils.resolveColor(this, R.attr.colorPrimary), 0,
                    UiUtils.resolveColor(this, R.attr.colorPrimaryDark), 0
            );
        } else {
            mSwipeLayout.setEnabled(false);
        }
    }

    protected ListAdapter getNavigationDrawerAdapter() {
        return null;
    }

    protected boolean isRightSideDrawer() {
        return false;
    }

    protected boolean onDrawerItemSelected(int position) {
        return false;
    }

    private void setupNavigationDrawer() {
        ListAdapter adapter = getNavigationDrawerAdapter();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);
        if (adapter != null) {
            ListView drawerList = (ListView) findViewById(R.id.drawer_list);
            drawerList.setAdapter(adapter);
            drawerList.setOnItemClickListener(new ListView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (onDrawerItemSelected(position)) {
                        mDrawerLayout.closeDrawers();
                    }
                }
            });

            if (isRightSideDrawer()) {
                DrawerLayout.LayoutParams lp =
                        (DrawerLayout.LayoutParams) drawerList.getLayoutParams();
                lp.gravity = Gravity.RIGHT;
                drawerList.setLayoutParams(lp);
            } else {
                mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0);
                mDrawerLayout.setDrawerListener(mDrawerToggle);
            }
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void setContentView(int layoutResId) {
        if (layoutResId == R.layout.error) {
            super.setContentView(layoutResId);
        } else {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View contentView = layoutInflater.inflate(layoutResId, null);
            setContentView(contentView);
        }
    }

    @Override
    public void setContentView(View view) {
        ensureContent();
        if (view == null) {
            throw new IllegalArgumentException("Content view can't be null");
        }
        if (mContentContainer instanceof ViewGroup) {
            ViewGroup contentContainer = (ViewGroup) mContentContainer;
            if (mContentView == null) {
                contentContainer.addView(view);
            } else {
                int index = contentContainer.indexOfChild(mContentView);
                // replace content view
                contentContainer.removeView(mContentView);
                contentContainer.addView(view, index);
            }
            mContentView = view;
        } else {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
    }

    protected void setEmptyText(int resId) {
        setEmptyText(getString(resId));
    }

    public void setEmptyText(CharSequence text) {
        ensureContent();
        if (mEmptyView != null && mEmptyView instanceof TextView) {
            TextView emptyView = (TextView) mEmptyView;
            emptyView.setText(text);
        } else {
            throw new IllegalStateException("Can't be used with a custom content view");
        }
    }

    protected void setContentShown(boolean shown) {
        setContentShown(shown, true);
    }

    private void setContentShown(boolean shown, boolean animate) {
        ensureContent();
        if (mContentShown == shown) {
            return;
        }
        mContentShown = shown;
        if (shown) {
            if (animate) {
                mProgress.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mContentContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            } else {
                mProgress.clearAnimation();
                mContentContainer.clearAnimation();
            }
            mProgress.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
        } else {
            if (animate) {
                mProgress.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
                mContentContainer.startAnimation(
                        AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            } else {
                mProgress.clearAnimation();
                mContentContainer.clearAnimation();
            }
            mProgress.setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);
        }
    }

    protected void setContentEmpty(boolean isEmpty) {
        ensureContent();
        if (mContentView == null) {
            throw new IllegalStateException("Content view must be initialized before");
        }
        if (isEmpty) {
            mEmptyView.setVisibility(View.VISIBLE);
            mContentView.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mContentView.setVisibility(View.VISIBLE);
        }
    }

    private void ensureContent() {
        if (mContentContainer != null && mProgress != null) {
            return;
        }
        mProgress = (SmoothProgressBar) findViewById(R.id.progress);
        if (mProgress == null) {
            throw new RuntimeException("Your content must have a ViewGroup whose id attribute is 'R.id.progress'");
        }
        mProgress.setSmoothProgressDrawableColors(new int[] {
            UiUtils.resolveColor(this, R.attr.colorPrimary),
            UiUtils.resolveColor(this, R.attr.colorPrimaryDark)
        });

        mContentContainer = findViewById(R.id.content_container);
        if (mContentContainer == null) {
            throw new RuntimeException("Your content must have a ViewGroup whose id attribute is 'R.id.content_container'");
        }
        mEmptyView = findViewById(android.R.id.empty);
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
        mContentShown = true;
    }
}
