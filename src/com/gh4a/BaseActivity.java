/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.gh4a.activities.Github4AndroidActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.ToastUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;
import com.shamanland.fab.FloatingActionButton;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class BaseActivity extends ActionBarActivity implements
        SwipeRefreshLayout.OnRefreshListener {
    private ViewGroup mContentContainer;
    private TextView mEmptyView;
    private boolean mContentShown;

    private View mHeader;
    private FrameLayout mOverlay;
    private FloatingActionButton mHeaderFab;
    private SmoothProgressBar mProgress;
    private SwipeRefreshLayout mSwipeLayout;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mHasErrorView = false;

    private ViewTreeObserver.OnGlobalLayoutListener mOverlayLayoutListener =
            new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mHeaderFab.getLayoutParams();
            params.topMargin = mHeader.getBottom() - mHeaderFab.getHeight() / 2;
            mHeaderFab.setLayoutParams(params);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        if (isOnline()) {
            super.setContentView(R.layout.base_activity);
            setupSwipeToRefresh();
            setupNavigationDrawer();
        } else {
            setErrorView();
        }
    }

    protected ListAdapter getNavigationDrawerAdapter() {
        return null;
    }

    protected boolean isRightSideDrawer() {
        return false;
    }

    protected void toggleDrawer() {
        int gravity = isRightSideDrawer() ? Gravity.RIGHT : Gravity.LEFT;
        if (mDrawerLayout.isDrawerOpen(gravity)) {
            mDrawerLayout.closeDrawer(gravity);
        } else {
            mDrawerLayout.openDrawer(gravity);
        }
    }

    protected boolean onDrawerItemSelected(int position) {
        return false;
    }

    protected boolean canSwipeToRefresh() {
        return false;
    }

    protected void refreshDone() {
        mSwipeLayout.setRefreshing(false);
    }

    protected void setChildScrollDelegate(SwipeRefreshLayout.ChildScrollDelegate delegate) {
        mSwipeLayout.setChildScrollDelegate(delegate);
    }

    protected void setEmptyText(int resId) {
        setEmptyText(getString(resId));
    }

    protected void setEmptyText(CharSequence text) {
        ensureContent();
        mEmptyView.setText(text);
    }

    protected void setContentShown(boolean shown) {
        setContentShown(shown, true);
    }

    protected void setContentEmpty(boolean isEmpty) {
        ensureContent();
        if (isEmpty) {
            mEmptyView.setVisibility(View.VISIBLE);
            mContentContainer.setVisibility(View.GONE);
        } else {
            mEmptyView.setVisibility(View.GONE);
            mContentContainer.setVisibility(View.VISIBLE);
        }
    }

    protected Intent navigateUp() {
        return null;
    }

    protected ProgressDialog showProgressDialog(String message, boolean cancelable) {
        return ProgressDialog.show(this, "", message, cancelable);
    }

    protected void stopProgressDialog(ProgressDialog progressDialog) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    protected void setHeaderAlignedActionButton(FloatingActionButton fab) {
        mHeaderFab = fab;

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.RIGHT;
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.content_padding);

        ensureContent();
        mOverlay.getViewTreeObserver().addOnGlobalLayoutListener(mOverlayLayoutListener);
        mOverlay.addView(fab, params);
    }

    protected void transitionHeaderToColor(int color) {
        View header = findViewById(R.id.header);
        TransitionDrawable transition = new TransitionDrawable(new Drawable[] {
            header.getBackground(),
            new ColorDrawable(color)
        });
        header.setBackgroundDrawable(transition);
        transition.startTransition(200);
    }

    protected boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        return info != null && info.isAvailable() && info.isConnected();
    }

    protected void goToToplevelActivity(boolean newTask) {
        Intent intent = getToplevelActivityIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (newTask) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        startActivity(intent);
    }

    protected Intent getToplevelActivityIntent() {
        Gh4Application app = Gh4Application.get(this);
        if (app.isAuthorized()) {
            Intent intent = IntentUtils.getUserActivityIntent(this, app.getAuthLogin(), null);
            intent.putExtra(UserActivity.EXTRA_TOPLEVEL_MODE, true);
            return intent;
        } else {
            return new Intent(this, Github4AndroidActivity.class);
        }
    }

    protected boolean hasErrorView() {
        return mHasErrorView;
    }

    protected void saveBookmark(String name, int type, Intent intent, String extraData) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarksProvider.Columns.NAME, name);
        cv.put(BookmarksProvider.Columns.TYPE, type);
        cv.put(BookmarksProvider.Columns.URI, intent.toUri(0));
        cv.put(BookmarksProvider.Columns.EXTRA, extraData);
        if (getContentResolver().insert(BookmarksProvider.Columns.CONTENT_URI, cv) != null) {
            ToastUtils.showMessage(this, R.string.bookmark_saved);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!hasErrorView()) {
            ensureContent();
            if (mContentContainer.getChildCount() == 0) {
                throw new IllegalStateException("Content view must be initialized before");
            }
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

        if (item.getItemId() == android.R.id.home) {
            Intent intent = navigateUp();
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            } else {
                finish();
            }
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
            View contentView = layoutInflater.inflate(layoutResId, mContentContainer, false);
            setContentView(contentView);
        }
    }

    @Override
    public void setContentView(View view) {
        ensureContent();
        if (view == null) {
            throw new IllegalArgumentException("Content view can't be null");
        }

        mContentContainer.removeAllViews();
        mContentContainer.addView(view);
    }

    @Override
    public boolean onSearchRequested() {
        Intent intent = new Intent(this, SearchActivity.class);
        startActivity(intent);
        return true;
    }

    @Override
    public void onRefresh() {
        mSwipeLayout.setRefreshing(true);
    }

    private void setErrorView() {
        mHasErrorView = true;
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.error);

        findViewById(R.id.btn_home).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                goToToplevelActivity(true);
            }
        });
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

    private void setupNavigationDrawer() {
        ListAdapter adapter = getNavigationDrawerAdapter();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_container);

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

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
                View drawer = findViewById(R.id.drawer);
                DrawerLayout.LayoutParams lp = (DrawerLayout.LayoutParams) drawer.getLayoutParams();
                lp.gravity = Gravity.RIGHT;
                drawer.setLayoutParams(lp);
            } else {
                mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolBar, 0, 0);
                mDrawerLayout.setDrawerListener(mDrawerToggle);

                ViewGroup title = (ViewGroup) findViewById(R.id.drawer_title);
                LayoutInflater.from(this).inflate(R.layout.drawer_title_main, title);
            }
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
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

    private void ensureContent() {
        if (mContentContainer != null && mProgress != null) {
            return;
        }
        mProgress = (SmoothProgressBar) findViewById(R.id.progress);
        mProgress.setSmoothProgressDrawableColors(new int[] {
                UiUtils.resolveColor(this, R.attr.colorPrimary),
                UiUtils.resolveColor(this, R.attr.colorPrimaryDark)
        });

        mContentContainer = (ViewGroup) findViewById(R.id.content_container);
        mEmptyView = (TextView) findViewById(android.R.id.empty);

        mOverlay = (FrameLayout) findViewById(R.id.overlay);
        mHeader = findViewById(R.id.header);

        mContentShown = true;
    }
}