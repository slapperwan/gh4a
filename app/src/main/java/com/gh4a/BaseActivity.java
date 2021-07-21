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

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.appbar.AppBarLayout;

import androidx.appcompat.view.ContextThemeWrapper;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;

import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.gh4a.activities.Github4AndroidActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.home.HomeActivity;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;
import com.gh4a.widget.ToggleableAppBarLayoutBehavior;
import com.meisolsson.githubsdk.model.ClientErrorResponse;
import com.philosophicalhacker.lib.RxLoader;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;
import io.reactivex.SingleTransformer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public abstract class BaseActivity extends AppCompatActivity implements
        SwipeRefreshLayout.OnRefreshListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        NavigationView.OnNavigationItemSelectedListener {
    public interface RefreshableChild {
        void onRefresh();
    }

    private ViewGroup mContentContainer;
    private TextView mEmptyView;
    private boolean mContentShown;
    private boolean mContentEmpty;
    private boolean mErrorShown;
    private boolean mCanSwipeToRefresh = true;

    private AppBarLayout mHeader;
    private ToggleableAppBarLayoutBehavior mHeaderBehavior;
    private SmoothProgressBar mProgress;
    private SwipeRefreshLayout mSwipeLayout;
    private DrawerLayout mDrawerLayout;
    private CoordinatorLayout mCoordinatorLayout;
    private Toolbar mToolbar;
    private NavigationView mRightDrawer;
    private View mLeftDrawerHeader;
    private View mRightDrawerHeader;
    private SupportMenuInflater mMenuInflater;

    private boolean mAppBarLocked = false;
    private boolean mAppBarScrollable = true;

    private ActivityCompat.OnRequestPermissionsResultCallback mPendingPermissionCb;

    private final List<ColorDrawable> mHeaderDrawables = new ArrayList<>();
    private final List<ColorDrawable> mStatusBarDrawables = new ArrayList<>();
    private final int[] mProgressColors = new int[2];
    private Animator mHeaderTransition;
    private final Handler mHandler = new Handler();

    private RxLoader mRxLoader;
    private final CompositeDisposable mDisposeOnStop = new CompositeDisposable();

    @SuppressWarnings("Convert2Lambda") // lambda wouldn't allow TargetApi annotation
    private final Runnable mUpdateTaskDescriptionRunnable = new Runnable() {
        @TargetApi(21)
        @Override
        public void run() {
            String label = IntentUtils.isNewTaskIntent(getIntent()) ? getActionBarTitle() : null;
            setTaskDescription(new ActivityManager.TaskDescription(label, null,
                    mProgressColors[0]));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            onInitExtras(extras);
        }
        super.onCreate(savedInstanceState);

        mRxLoader = new RxLoader(this, LoaderManager.getInstance(this));

        super.setContentView(R.layout.base_activity);

        setupSwipeToRefresh();
        setupNavigationDrawer();
        setupHeaderDrawable();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(getActionBarTitle());
        actionBar.setSubtitle(getActionBarSubtitle());
        actionBar.setDisplayHomeAsUpEnabled(!IntentUtils.isNewTaskIntent(getIntent()));

        scheduleTaskDescriptionUpdate();
    }

    @Nullable
    protected String getActionBarTitle() {
        return null;
    }

    @Nullable
    protected String getActionBarSubtitle() {
        return null;
    }

    public void handleLoadFailure(Throwable e) {
        handleFailure("Loading data failed", e);
        setErrorViewVisibility(true, e);
    }

    public void handleActionFailure(String text, Throwable e) {
        handleFailure(text, e);
    }

    private void handleFailure(String text, Throwable e) {
        ApiRequestException are = e instanceof ApiRequestException ? (ApiRequestException) e : null;
        boolean isAuthError = are != null && are.getStatus() == HttpURLConnection.HTTP_UNAUTHORIZED;
        if (isAuthError) {
            Snackbar.make(mCoordinatorLayout, R.string.load_auth_failure_notice, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.login, v -> goToToplevelActivity())
                    .show();
        }
        if (are == null && e instanceof RuntimeException) {
            // If this happens, it means Rx catched a programming error of us. Crash the app
            // in that case, as that's what would have happened without Rx as well.
            // In doing so, don't just throw the exception (but instead delegate it right to the
            // uncaught exception handler) to make sure RX doesn't add its composite exception
            // which obscures the actual stack trace.
            Thread currentThread = Thread.currentThread();
            Thread.UncaughtExceptionHandler handler = currentThread.getUncaughtExceptionHandler();
            handler.uncaughtException(currentThread, e);
        }
        Log.d(Gh4Application.LOG_TAG, text, e);
    }

    protected void registerTemporarySubscription(Disposable disposable) {
        mDisposeOnStop.add(disposable);
    }

    protected int getLeftNavigationDrawerMenuResource() {
        return 0;
    }

    @IdRes
    protected int getInitialLeftDrawerSelection(Menu menu) {
        return 0;
    }

    protected int[] getRightNavigationDrawerMenuResources() {
        return null;
    }

    @IdRes
    protected int getInitialRightDrawerSelection() {
        return 0;
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

    protected void configureLeftDrawerHeader(View header) {
    }

    protected boolean canSwipeToRefresh() {
        return mCanSwipeToRefresh;
    }

    public void setCanSwipeToRefresh(boolean canSwipeToRefresh) {
        mCanSwipeToRefresh = canSwipeToRefresh;
        updateSwipeToRefreshState();
    }

    protected void setChildScrollDelegate(SwipeRefreshLayout.ChildScrollDelegate delegate) {
        mSwipeLayout.setChildScrollDelegate(delegate);
    }

    protected void setEmptyText(CharSequence text) {
        ensureContent();
        mEmptyView.setText(text);
    }

    protected void setContentShown(boolean shown) {
        mContentShown = shown;
        updateSwipeToRefreshState();
        updateViewVisibility(true);
    }

    protected void setContentEmpty(boolean isEmpty) {
        mContentEmpty = isEmpty;
        updateViewVisibility(false);
    }

    protected Intent navigateUp() {
        return null;
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

    protected void transitionHeaderToColor(int colorAttrId, int statusBarColorAttrId) {
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

        progressAnimator1.addUpdateListener(anim -> {
            mProgressColors[0] = (int) progressAnimator1.getAnimatedValue();
            mProgressColors[1] = (int) progressAnimator2.getAnimatedValue();
            mProgress.invalidate();
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

            int initialRightDrawerSelection = getInitialRightDrawerSelection();
            if (initialRightDrawerSelection != 0) {
                mRightDrawer.setCheckedItem(initialRightDrawerSelection);
            }

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

    public int getAppBarTotalScrollRange() {
        return mHeader.getTotalScrollRange();
    }

    public void removeAppBarOffsetListener(AppBarLayout.OnOffsetChangedListener l) {
        mHeader.removeOnOffsetChangedListener(l);
    }

    public void collapseAppBar() {
        mHeader.setExpanded(false);
    }

    public void setAppBarLocked(boolean locked) {
        mAppBarLocked = locked;
        updateAppBarEnabledState();
    }

    protected void addHeaderView(View view, boolean scrollable) {
        mHeader.addView(view, 1, new AppBarLayout.LayoutParams(
                AppBarLayout.LayoutParams.MATCH_PARENT,
                AppBarLayout.LayoutParams.WRAP_CONTENT));
        setAppBarChildScrollable(view, scrollable);
    }

    protected void setToolbarScrollable(boolean scrollable) {
        setAppBarChildScrollable(mToolbar, scrollable);
        mAppBarScrollable = scrollable;
        updateAppBarEnabledState();
    }

    private void updateAppBarEnabledState() {
        mHeaderBehavior.setEnabled(mAppBarScrollable && !mAppBarLocked);
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

    @Override
    protected void onStart() {
        super.onStart();
        ensureContent();
        if (mContentContainer.getChildCount() == 0) {
            throw new IllegalStateException("Content view must be initialized before");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDisposeOnStop.clear();
    }

    @NonNull
    @Override
    public MenuInflater getMenuInflater() {
        if (mMenuInflater == null) {
            mMenuInflater = new SupportMenuInflater(
                    new ContextThemeWrapper(this, R.style.HeaderTheme));
        }
        return mMenuInflater;
    }

    @Override
    @CallSuper
    public boolean onCreateOptionsMenu(Menu menu) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                && !IntentUtils.isNewTaskIntent(getIntent())
                && displayDetachAction()) {
            menu.add(Menu.NONE, R.id.detach, Menu.NONE, R.string.detach);
        }

        return super.onCreateOptionsMenu(menu);
    }

    public boolean displayDetachAction() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

        if (item.getItemId() == R.id.detach) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                IntentUtils.startNewTask(this, getIntent());
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
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
        startActivity(SearchActivity.makeIntent(this));
        return true;
    }

    @Override
    public void onRefresh() {
        supportInvalidateOptionsMenu();
        mSwipeLayout.setRefreshing(false);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        mDrawerLayout.closeDrawers();
        return false;
    }

    public void setRightDrawerLockedClosed(boolean locked) {
        mDrawerLayout.setDrawerLockMode(
                locked ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED,
                Gravity.RIGHT);
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
                        .setAction(R.string.ok, v -> ActivityCompat.requestPermissions(
                                BaseActivity.this, new String[] { permission }, 0))
                        .show();
            } else {
                ActivityCompat.requestPermissions(this, new String[] { permission }, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (mPendingPermissionCb != null) {
            mPendingPermissionCb.onRequestPermissionsResult(0, permissions, grantResults);
            mPendingPermissionCb = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (closeDrawers()) {
            return;
        }
        boolean wasStartedAsNewTask = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_NEW_TASK) != 0;
        if (wasStartedAsNewTask && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask();
        } else {
            super.onBackPressed();
        }
    }

    public <T> SingleTransformer<T, T> makeLoaderSingle(int id, boolean force) {
        return upstream -> upstream
                .compose(RxUtils::doInBackground)
                .compose(mRxLoader.makeSingleTransformer(id, force));
    }

    protected void setErrorViewVisibility(boolean visible, Throwable e) {
        View content = findViewById(R.id.content);
        View error = findViewById(R.id.error);

        content.setVisibility(visible ? View.GONE : View.VISIBLE);
        mErrorShown = visible;
        updateSwipeToRefreshState();

        if (error == null) {
            if (!visible) {
                // It's not inflated yet and we don't want it
                // to be visible, so there's nothing to do
                return;
            }
            ViewStub errorStub = findViewById(R.id.error_stub);
            error = errorStub.inflate();

            error.findViewById(R.id.retry_button).setOnClickListener(v -> {
                setErrorViewVisibility(false, null);
                onRefresh();
            });
        }

        if (visible) {
            View retryButton = error.findViewById(R.id.retry_button);
            TextView messageView = error.findViewById(R.id.error_message);

            ApiRequestException re = e instanceof ApiRequestException ? (ApiRequestException) e : null;
            ClientErrorResponse.BlockReason blockReason = re != null && re.getResponse() != null
                    ? re.getResponse().blockReason() : null;

            if (blockReason != null) {
                messageView.setText(
                        getString(R.string.load_failure_explanation_dmca, blockReason.htmlUrl()));
                retryButton.setVisibility(View.GONE);
            } else if (re != null && re.getMessage() != null) {
                messageView.setText(
                        getString(R.string.load_failure_explanation_with_reason, re.getMessage()));
                retryButton.setVisibility(View.VISIBLE);
            } else {
                messageView.setText(R.string.load_failure_explanation);
                retryButton.setVisibility(View.VISIBLE);
            }
            error.setVisibility(View.VISIBLE);
        } else {
            error.setVisibility(View.GONE);
        }
    }

    protected void onDrawerOpened(boolean right) {
    }

    protected void onDrawerClosed(boolean right) {
    }

    private void updateSwipeToRefreshState() {
        ensureContent();
        mSwipeLayout.setEnabled(mContentShown && !mErrorShown && canSwipeToRefresh());
    }

    private void setupHeaderDrawable() {
        ensureContent();

        int primaryColor = UiUtils.resolveColor(this, R.attr.colorPrimary);
        assignBackground(mLeftDrawerHeader, primaryColor);
        assignBackground(mRightDrawerHeader, primaryColor);
        assignBackground(mHeader, primaryColor);

        int primaryDarkColor = UiUtils.resolveColor(this, R.attr.colorPrimaryDark);
        ColorDrawable d = new ColorDrawable(primaryDarkColor);
        mDrawerLayout.setStatusBarBackground(d);
        mStatusBarDrawables.add(d);
    }

    private ObjectAnimator createColorTransition(ColorDrawable drawable, int color) {
        final ObjectAnimator animation = ObjectAnimator.ofInt(drawable,
                "color", drawable.getColor(), color);
        animation.setEvaluator(new ArgbEvaluator());
        return animation;
    }

    private void assignBackground(View view, int color) {
        if (view == null) {
            return;
        }
        ColorDrawable background = new ColorDrawable(color);
        view.setBackground(background);
        mHeaderDrawables.add(background);
    }

    private void setupSwipeToRefresh() {
        mSwipeLayout = findViewById(R.id.swipe_container);
        if (canSwipeToRefresh()) {
            mSwipeLayout.setOnRefreshListener(this);
            mSwipeLayout.setColorSchemeColors(
                    UiUtils.resolveColor(this, R.attr.colorPrimary), 0,
                    UiUtils.resolveColor(this, R.attr.colorPrimaryDark), 0
            );
        }

        CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) mSwipeLayout.getLayoutParams();
        lp.setBehavior(onCreateSwipeLayoutBehavior());

        updateSwipeToRefreshState();
    }

    protected AppBarLayout.ScrollingViewBehavior onCreateSwipeLayoutBehavior() {
        return new AppBarLayout.ScrollingViewBehavior();
    }

    private void setupNavigationDrawer() {
        NavigationView leftDrawer = findViewById(R.id.left_drawer);
        applyHighlightColor(leftDrawer);
        mRightDrawer = findViewById(R.id.right_drawer);
        applyHighlightColor(mRightDrawer);

        mDrawerLayout = findViewById(R.id.drawer_container);
        mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }
            @Override
            public void onDrawerOpened(View drawerView) {
                BaseActivity.this.onDrawerOpened(drawerView == mRightDrawer);
            }
            @Override
            public void onDrawerClosed(View drawerView) {
                BaseActivity.this.onDrawerClosed(drawerView == mRightDrawer);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });

        Toolbar toolBar = findViewById(R.id.toolbar);
        setSupportActionBar(toolBar);

        int drawerMenuResId = getLeftNavigationDrawerMenuResource();
        if (drawerMenuResId != 0) {
            leftDrawer.inflateMenu(drawerMenuResId);
            leftDrawer.setNavigationItemSelectedListener(this);

            int initialLeftDrawerSelection = getInitialLeftDrawerSelection(leftDrawer.getMenu());
            if (initialLeftDrawerSelection != 0) {
                leftDrawer.setCheckedItem(initialLeftDrawerSelection);
            }

            ActionBar supportActionBar = getSupportActionBar();
            if (supportActionBar != null) {
                supportActionBar.setDisplayHomeAsUpEnabled(true);
            }

            toolBar.setNavigationIcon(new DrawerArrowDrawable(toolBar.getContext()));
            toolBar.setNavigationOnClickListener(v -> {
                int drawerLockMode = mDrawerLayout.getDrawerLockMode(GravityCompat.START);
                if (drawerLockMode != DrawerLayout.LOCK_MODE_LOCKED_CLOSED) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });

            mLeftDrawerHeader = leftDrawer.inflateHeaderView(R.layout.drawer_header_left);
            configureLeftDrawerHeader(mLeftDrawerHeader);
        } else {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, Gravity.LEFT);
        }

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_left, Gravity.LEFT);
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow_right, Gravity.RIGHT);
        mDrawerLayout.setScrimColor(ContextCompat.getColor(this, R.color.drawer_scrim));

        mRightDrawerHeader = mRightDrawer.inflateHeaderView(R.layout.drawer_header_right);

        updateRightNavigationDrawer();
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
        if (!getTheme().resolveAttribute(R.attr.colorAccent, value, true)) {
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
        updateViewVisibility(mProgress, animate, !mContentShown);
        updateViewVisibility(mEmptyView, animate, mContentEmpty && mContentShown);
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
        mProgress = findViewById(R.id.progress);
        mProgressColors[0] = UiUtils.resolveColor(this, R.attr.colorPrimary);
        mProgressColors[1] = UiUtils.resolveColor(this, R.attr.colorPrimaryDark);
        mProgress.setSmoothProgressDrawableColors(mProgressColors);

        mCoordinatorLayout = findViewById(R.id.coordinator_layout);
        mContentContainer = findViewById(R.id.content_container);
        mEmptyView = findViewById(android.R.id.empty);

        mHeader = findViewById(R.id.header);
        mToolbar = findViewById(R.id.toolbar);

        mHeaderBehavior = new ToggleableAppBarLayoutBehavior();
        CoordinatorLayout.LayoutParams lp =
                (CoordinatorLayout.LayoutParams) mHeader.getLayoutParams();
        lp.setBehavior(mHeaderBehavior);

        mSwipeLayout.setAppBarLayout(mHeader);

        mContentShown = true;
    }
}
