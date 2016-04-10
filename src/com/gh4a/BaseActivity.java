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

import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.gh4a.activities.Github4AndroidActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.ColorDrawable;
import com.gh4a.widget.SwipeRefreshLayout;
import com.gh4a.widget.ToggleableAppBarLayoutBehavior;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorSet;
import com.nineoldandroids.animation.ArgbEvaluator;
import com.nineoldandroids.animation.ObjectAnimator;
import com.nineoldandroids.animation.ValueAnimator;

import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public abstract class BaseActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener, DrawerLayout.DrawerListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        LoaderCallbacks.ParentCallback,
        NavigationView.OnNavigationItemSelectedListener {
    private ViewGroup mContentContainer;
    private TextView mEmptyView;
    private boolean mContentShown;
    private boolean mContentEmpty;

    private AppBarLayout mHeader;
    private ToggleableAppBarLayoutBehavior mHeaderBehavior;
    private SmoothProgressBar mProgress;
    private SwipeRefreshLayout mSwipeLayout;
    private DrawerLayout mDrawerLayout;
    private CoordinatorLayout mCoordinatorLayout;
    private Toolbar mToolbar;
    private ActionBarDrawerToggle mDrawerToggle;
    private NavigationView mLeftDrawer;
    private NavigationView mRightDrawer;
    private View mLeftDrawerTitle;
    private View mRightDrawerTitle;

    private ActivityCompat.OnRequestPermissionsResultCallback mPendingPermissionCb;

    private final List<ColorDrawable> mHeaderDrawables = new ArrayList<>();
    private final List<ColorDrawable> mStatusBarDrawables = new ArrayList<>();
    private final int[] mProgressColors = new int[2];
    private Animator mHeaderTransition;
    private Handler mHandler = new Handler();

    private Runnable mUpdateTaskDescriptionRunnable = new Runnable() {
        private String mLabel;
        private Bitmap mIcon;

        @TargetApi(21)
        @Override
        public void run() {
            if (mIcon == null) {
                mLabel = getString(R.string.app_name);
                mIcon = BitmapFactory.decodeResource(getResources(), R.drawable.octodroid);
            }
            ActivityManager.TaskDescription desc = new ActivityManager.TaskDescription(
                    mLabel, mIcon, mProgressColors[0]);
            setTaskDescription(desc);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        onInitExtras(getIntent().getExtras());
        super.onCreate(savedInstanceState);

        super.setContentView(R.layout.base_activity);

        setupSwipeToRefresh();
        setupNavigationDrawer();
        setupHeaderDrawable();
    }

    @Override
    public BaseActivity getBaseActivity() {
        return this;
    }

    public void handleAuthFailureDuringLoad() {
        Gh4Application.get().logout();
        Snackbar.make(mCoordinatorLayout, R.string.load_auth_failure_notice, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.login, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        goToToplevelActivity();
                    }
                })
                .show();
    }

    public void handleLoadFailure(Exception e) {
        setErrorViewVisibility(true);
    }

    protected int getLeftNavigationDrawerMenuResource() {
        return 0;
    }

    protected int[] getRightNavigationDrawerMenuResources() {
        return null;
    }

    protected boolean closeDrawers() {
        boolean result = false;
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
            result = true;
        }
        if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
            result = true;
        }
        return result;
    }

    protected void toggleRightSideDrawer() {
        if (mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
            mDrawerLayout.closeDrawer(Gravity.RIGHT);
        } else {
            mDrawerLayout.openDrawer(Gravity.RIGHT);
        }
    }

    protected View getLeftDrawerTitle(ViewGroup container) {
        return getLayoutInflater().inflate(R.layout.drawer_title_main, container, false);
    }

    protected boolean canSwipeToRefresh() {
        return true;
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
        mContentEmpty = isEmpty;
        updateViewVisibility(false);
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

    public CoordinatorLayout getRootLayout() {
        ensureContent();
        return mCoordinatorLayout;
    }

    protected void onInitExtras(Bundle extras) {

    }

    protected void setHeaderColor(int color, int statusBarColor) {
        cancelHeaderTransition();

        for (ColorDrawable d : mHeaderDrawables) {
            d.setColor(color);
        }
        for (ColorDrawable d : mStatusBarDrawables) {
            d.setColor(statusBarColor);
        }
        mProgressColors[0] = color;
        mProgressColors[1] = statusBarColor;
        mProgress.invalidate();
        scheduleTaskDescriptionUpdate();
    }

    public void transitionHeaderToColor(int colorAttrId, int statusBarColorAttrId) {
        final AnimatorSet animation = new AnimatorSet();
        List<Animator> animators = new ArrayList<>();
        int color = UiUtils.resolveColor(this, colorAttrId);
        int statusBarColor = UiUtils.resolveColor(this, statusBarColorAttrId);

        for (ColorDrawable d : mHeaderDrawables) {
            animators.add(createColorTransition(d, color));
        }
        for (ColorDrawable d : mStatusBarDrawables) {
            animators.add(createColorTransition(d, statusBarColor));
        }

        final ValueAnimator progressAnimator1 = ValueAnimator.ofInt(mProgressColors[0], color);
        progressAnimator1.setEvaluator(new ArgbEvaluator());
        animators.add(progressAnimator1);
        final ValueAnimator progressAnimator2 = ValueAnimator.ofInt(mProgressColors[1], statusBarColor);
        progressAnimator2.setEvaluator(new ArgbEvaluator());
        animators.add(progressAnimator2);

        progressAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgressColors[0] = (int) progressAnimator1.getAnimatedValue();
                mProgressColors[1] = (int) progressAnimator2.getAnimatedValue();
                mProgress.invalidate();
            }
        });

        cancelHeaderTransition();

        animation.playTogether(animators);
        animation.setDuration(200);
        animation.start();
        mHeaderTransition = animation;
        scheduleTaskDescriptionUpdate();
    }

    private void cancelHeaderTransition() {
        if (mHeaderTransition != null && mHeaderTransition.isRunning()) {
            mHeaderTransition.cancel();
        }
        mHeaderTransition = null;
    }

    private void scheduleTaskDescriptionUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mHandler.removeCallbacks(mUpdateTaskDescriptionRunnable);
            mHandler.postDelayed(mUpdateTaskDescriptionRunnable, 500);
        }
    }

    protected void updateRightNavigationDrawer() {
        int[] drawerMenuResIds = getRightNavigationDrawerMenuResources();
        if (drawerMenuResIds != null) {
            mRightDrawer.getMenu().clear();
            for (int id : drawerMenuResIds) {
                mRightDrawer.inflateMenu(id);
            }
            mRightDrawer.setNavigationItemSelectedListener(this);
            onPrepareRightNavigationDrawerMenu(mRightDrawer.getMenu());

            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, Gravity.RIGHT);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.RIGHT);
        }
    }

    protected void onPrepareRightNavigationDrawerMenu(Menu menu) {

    }

    protected void goToToplevelActivity() {
        Intent intent = getToplevelActivityIntent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    protected Intent getToplevelActivityIntent() {
        Gh4Application app = Gh4Application.get();
        if (app.isAuthorized()) {
            return new Intent(this, HomeActivity.class);
        } else {
            return new Intent(this, Github4AndroidActivity.class);
        }
    }

    protected SharedPreferences getPrefs() {
        return getSharedPreferences(SettingsFragment.PREF_NAME, MODE_PRIVATE);
    }

    public void addAppBarOffsetListener(AppBarLayout.OnOffsetChangedListener l) {
        mHeader.addOnOffsetChangedListener(l);
    }

    public void removeAppBarOffsetListener(AppBarLayout.OnOffsetChangedListener l) {
        mHeader.removeOnOffsetChangedListener(l);
    }

    protected void addHeaderView(View view, boolean scrollable) {
        mHeader.addView(view, 1, new AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.WRAP_CONTENT));
        setAppBarChildScrollable(view, scrollable);
    }

    protected void setToolbarScrollable(boolean scrollable) {
        setAppBarChildScrollable(mToolbar, scrollable);
        mHeaderBehavior.setEnabled(scrollable);
    }

    private void setAppBarChildScrollable(View view, boolean scrollable) {
        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) view.getLayoutParams();
        if (scrollable) {
            lp.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        } else {
            lp.setScrollFlags(0);
        }
        view.setLayoutParams(lp);
    }

    protected void saveBookmark(String name, int type, Intent intent, String extraData) {
        ContentValues cv = new ContentValues();
        cv.put(BookmarksProvider.Columns.NAME, name);
        cv.put(BookmarksProvider.Columns.TYPE, type);
        cv.put(BookmarksProvider.Columns.URI, intent.toUri(0));
        cv.put(BookmarksProvider.Columns.EXTRA, extraData);
        if (getContentResolver().insert(BookmarksProvider.Columns.CONTENT_URI, cv) != null) {
            Toast.makeText(this, R.string.bookmark_saved, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ensureContent();
        if (mContentContainer.getChildCount() == 0) {
            throw new IllegalStateException("Content view must be initialized before");
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
        ensureContent();
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View contentView = layoutInflater.inflate(layoutResId, mContentContainer, false);
        setContentView(contentView);
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
        supportInvalidateOptionsMenu();
        mSwipeLayout.setRefreshing(false);
    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (mDrawerToggle != null && drawerView == mLeftDrawer) {
            mDrawerToggle.onDrawerOpened(drawerView);
        }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (mDrawerToggle != null && drawerView == mLeftDrawer) {
            mDrawerToggle.onDrawerClosed(drawerView);
        }
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {
        if (mDrawerToggle != null && drawerView == mLeftDrawer) {
            mDrawerToggle.onDrawerSlide(drawerView, slideOffset);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        if (mDrawerToggle != null) {
            mDrawerToggle.onDrawerStateChanged(newState);
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        mDrawerLayout.closeDrawers();
        return false;
    }

    public void requestPermission(final String permission,
            ActivityCompat.OnRequestPermissionsResultCallback cb,
            int rationaleTextResId) {
        if (mPendingPermissionCb != null) {
            throw new IllegalStateException();
        }
        int grantResult = ActivityCompat.checkSelfPermission(this, permission);
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            cb.onRequestPermissionsResult(0, new String[] { permission }, new int[] { grantResult });
        } else {
            mPendingPermissionCb = cb;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                Snackbar.make(getRootLayout(), rationaleTextResId, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(BaseActivity.this,
                                        new String[] { permission }, 0);

                            }
                        })
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] { permission }, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (mPendingPermissionCb != null) {
            mPendingPermissionCb.onRequestPermissionsResult(0, permissions, grantResults);
            mPendingPermissionCb = null;
        }
    }

    protected void setErrorViewVisibility(boolean visible) {
        View content = findViewById(R.id.content);
        View error = findViewById(R.id.error);

        content.setVisibility(visible ? View.GONE : View.VISIBLE);
        mSwipeLayout.setEnabled(visible ? false : canSwipeToRefresh());

        if (error == null) {
            if (!visible) {
                // It's not inflated yet and we don't want it
                // to be visible, so there's nothing to do
                return;
            }
            ViewStub errorStub = (ViewStub) findViewById(R.id.error_stub);
            error = errorStub.inflate();

            error.findViewById(R.id.retry_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setErrorViewVisibility(false);
                    onRefresh();
                }
            });
        }
        error.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setupHeaderDrawable() {
        ensureContent();

        int primaryColor = UiUtils.resolveColor(this, R.attr.colorPrimary);
        assignBackground(mLeftDrawerTitle, primaryColor);
        assignBackground(mRightDrawerTitle, primaryColor);
        assignBackground(mHeader, primaryColor);

        int primaryDarkColor = UiUtils.resolveColor(this, R.attr.colorPrimaryDark);
        ColorDrawable d = ColorDrawable.create(primaryDarkColor);
        mDrawerLayout.setStatusBarBackground(d);
        mStatusBarDrawables.add(d);
    }

    private ObjectAnimator createColorTransition(ColorDrawable drawable, int color) {
        final ObjectAnimator animation = ObjectAnimator.ofInt(drawable,
                "color", drawable.getColor(), color);
        animation.setEvaluator(new ArgbEvaluator());
        return animation;
    }

    @SuppressWarnings("deprecation")
    private void assignBackground(View view, int color) {
        if (view == null) {
            return;
        }
        ColorDrawable background = ColorDrawable.create(color);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
        mHeaderDrawables.add(background);
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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_container);
        mLeftDrawer = (NavigationView) findViewById(R.id.left_drawer);
        applyHighlightColor(mLeftDrawer);
        mRightDrawer = (NavigationView) findViewById(R.id.right_drawer);
        applyHighlightColor(mRightDrawer);

        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        int drawerMenuResId = getLeftNavigationDrawerMenuResource();
        if (drawerMenuResId != 0) {
            mLeftDrawer.inflateMenu(drawerMenuResId);
            mLeftDrawer.setNavigationItemSelectedListener(this);

            mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolBar, 0, 0);
            mDrawerLayout.setDrawerListener(this);

            mLeftDrawerTitle = getLeftDrawerTitle(mLeftDrawer);
            if (mLeftDrawerTitle!= null) {
                mLeftDrawer.addHeaderView(mLeftDrawerTitle);
            }
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_left, Gravity.LEFT);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_right, Gravity.RIGHT);
        mDrawerLayout.setScrimColor(ContextCompat.getColor(this, R.color.drawer_scrim));

        mRightDrawerTitle = mRightDrawer.inflateHeaderView(R.layout.drawer_title_right);

        updateRightNavigationDrawer();
    }

    private void setContentShown(boolean shown, boolean animate) {
        mContentShown = shown;
        updateViewVisibility(animate);
    }

    private void applyHighlightColor(NavigationView view) {
        ColorStateList iconTint =
                createDefaultNavigationColorStateList(android.R.attr.textColorSecondary);
        if (iconTint != null) {
            view.setItemIconTintList(iconTint);
        }
        ColorStateList textColor =
                createDefaultNavigationColorStateList(android.R.attr.textColorPrimary);
        if (textColor != null) {
            view.setItemTextColor(textColor);
        }
    }

    // similar to what NavigationView does by default,
    // but uses accent color instead of primary color
    private ColorStateList createDefaultNavigationColorStateList(int baseColorThemeAttr) {
        TypedValue value = new TypedValue();
        if (!getTheme().resolveAttribute(baseColorThemeAttr, value, true)) {
            return null;
        }
        ColorStateList baseColor = ContextCompat.getColorStateList(this, value.resourceId);
        if (!getTheme().resolveAttribute(android.support.design.R.attr.colorAccent, value, true)) {
            return null;
        }
        int colorAccent = value.data;
        int defaultColor = baseColor.getDefaultColor();
        final int[] disabledStateSet = { -android.R.attr.state_enabled };
        final int[] checkedStateSet = { android.R.attr.state_checked };
        final int[][] states = { disabledStateSet, checkedStateSet, { 0 } };
        final int[] colors = {
            baseColor.getColorForState(disabledStateSet, defaultColor),
            colorAccent,
            defaultColor
        };

        return new ColorStateList(states, colors);
    }

    private void updateViewVisibility(boolean animate) {
        ensureContent();
        updateViewVisibility(mProgress, animate, !mContentEmpty && !mContentShown);
        updateViewVisibility(mEmptyView, animate, mContentEmpty);
        updateViewVisibility(mContentContainer, animate, !mContentEmpty && mContentShown);
    }

    private void updateViewVisibility(View view, boolean animate, boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;
        if (view.getVisibility() == visibility) {
            return;
        }

        if (animate) {
            Animation anim = AnimationUtils.loadAnimation(view.getContext(),
                    show ? android.R.anim.fade_in : android.R.anim.fade_out);
            view.startAnimation(anim);
        } else {
            view.clearAnimation();
        }
        view.setVisibility(visibility);
    }

    private void ensureContent() {
        if (mContentContainer != null && mProgress != null) {
            return;
        }
        mProgress = (SmoothProgressBar) findViewById(R.id.progress);
        mProgressColors[0] = UiUtils.resolveColor(this, R.attr.colorPrimary);
        mProgressColors[1] = UiUtils.resolveColor(this, R.attr.colorPrimaryDark);
        mProgress.setSmoothProgressDrawableColors(mProgressColors);

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        mContentContainer = (ViewGroup) findViewById(R.id.content_container);
        mEmptyView = (TextView) findViewById(android.R.id.empty);

        mHeader = (AppBarLayout) findViewById(R.id.header);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);

        mHeaderBehavior = new ToggleableAppBarLayoutBehavior();
        CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) mHeader.getLayoutParams();
        lp.setBehavior(mHeaderBehavior);

        mSwipeLayout.setAppBarLayout(mHeader);

        mContentShown = true;
    }
}