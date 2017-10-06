package com.gh4a.activities.home;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.Github4AndroidActivity;
import com.gh4a.activities.SettingsActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.fragment.NotificationListFragment;
import com.gh4a.fragment.RepositoryListContainerFragment;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.HasNotificationsLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.User;

public class HomeActivity extends BaseFragmentPagerActivity implements
        View.OnClickListener, RepositoryListContainerFragment.Callback,
        NotificationListFragment.ParentCallback {
    public static Intent makeIntent(Context context, @IdRes int initialPageId) {
        String initialPage = START_PAGE_MAPPING.get(initialPageId);
        Intent intent = new Intent(context, HomeActivity.class);
        if (initialPage != null) {
            intent.putExtra("initial_page", initialPage);
        }
        return intent;
    }

    public static Intent makeNotificationsIntent(Context context, String repoOwner,
            String repoName) {
        return makeIntent(context, R.id.notifications)
                .putExtra(NotificationListFragment.EXTRA_INITIAL_REPO_OWNER, repoOwner)
                .putExtra(NotificationListFragment.EXTRA_INITIAL_REPO_NAME, repoName);
    }

    private static final int REQUEST_SETTINGS = 10000;

    private FragmentFactory mFactory;
    private ImageView mAvatarView;
    private TextView mUserExtraView;
    private ImageView mDrawerSwitcher;
    private String mUserLogin;
    private User mUserInfo;
    private int mSelectedFactoryId;
    private boolean mDrawerInAccountMode;
    private Menu mLeftDrawerMenu;
    private ImageView mNotificationsIndicator;
    private MenuItem mNotificationsMenuItem;
    private Drawable mNotificationsIndicatorIcon;

    private static final String STATE_KEY_FACTORY_ITEM = "factoryItem";

    private static final int OTHER_ACCOUNTS_GROUP_BASE_ID = 1000;

    private static final SparseArray<String> START_PAGE_MAPPING = new SparseArray<>();
    static {
        START_PAGE_MAPPING.put(R.id.news_feed, "newsfeed");
        START_PAGE_MAPPING.put(R.id.notifications, "notifications");
        START_PAGE_MAPPING.put(R.id.my_repos, "repos");
        START_PAGE_MAPPING.put(R.id.my_issues, "issues");
        START_PAGE_MAPPING.put(R.id.my_prs, "prs");
        START_PAGE_MAPPING.put(R.id.my_gists, "gists");
        START_PAGE_MAPPING.put(R.id.pub_timeline, "timeline");
        START_PAGE_MAPPING.put(R.id.trend, "trends");
        START_PAGE_MAPPING.put(R.id.blog, "blog");
        START_PAGE_MAPPING.put(R.id.bookmarks, "bookmarks");
        START_PAGE_MAPPING.put(R.id.search, "search");
    }

    private final LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>(this) {
        @Override
        protected Loader<LoaderResult<User>> onCreateLoader() {
            return new UserLoader(HomeActivity.this, mUserLogin);
        }
        @Override
        protected void onResultReady(User result) {
            Gh4Application.get().setCurrentAccountInfo(result);
            mUserInfo = result;
            updateUserInfo();
        }
    };

    private final LoaderCallbacks<Boolean> mHasNotificationsCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new HasNotificationsLoader(HomeActivity.this);
        }
        @Override
        protected void onResultReady(Boolean result) {
            setNotificationsIndicatorVisible(result);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mUserLogin = Gh4Application.get().getAuthLogin();
        if (savedInstanceState != null) {
            mSelectedFactoryId = savedInstanceState.getInt(STATE_KEY_FACTORY_ITEM);
        } else {
            mSelectedFactoryId = determineInitialPage();
        }
        mFactory = getFactoryForItem(mSelectedFactoryId);

        mNotificationsIndicatorIcon =
                DrawableCompat.wrap(ContextCompat.getDrawable(this, R.drawable.circle).mutate());

        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        getSupportLoaderManager().initLoader(0, null, mUserCallback);
        getSupportLoaderManager().initLoader(1, null, mHasNotificationsCallback);
    }

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(mFactory.getTitleResId());
    }

    private void updateNotificationIndicator(int checkedItemId) {
        if (mNotificationsIndicator == null) {
            return;
        }

        @ColorRes  int colorResId = checkedItemId == R.id.notifications
                ? R.attr.colorAccent : android.R.attr.textColorPrimary;
        @ColorInt int tint = UiUtils.resolveColor(this, colorResId);
        DrawableCompat.setTint(mNotificationsIndicatorIcon, tint);
        mNotificationsIndicator.setImageDrawable(mNotificationsIndicatorIcon);
    }

    public void setNotificationsIndicatorVisible(boolean visible) {
        if (mNotificationsIndicator != null) {
            mNotificationsIndicator.setVisibility(visible ? View.VISIBLE : View.GONE);
            mNotificationsMenuItem.setIcon(visible
                    ? R.drawable.icon_notifications_unread
                    : R.drawable.icon_notifications);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY_FACTORY_ITEM, mSelectedFactoryId);
        mFactory.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mFactory.onRestoreInstanceState(savedInstanceState);
        }
    }

    @Override
    public void onClick(View view) {
        updateDrawerMode(!mDrawerInAccountMode);
    }

    @Override
    protected int getLeftNavigationDrawerMenuResource() {
        return R.menu.home_nav_drawer;
    }

    @Override
    protected int getInitialLeftDrawerSelection(Menu menu) {
        mLeftDrawerMenu = menu;

        mNotificationsMenuItem = menu.findItem(R.id.notifications);
        if (mNotificationsMenuItem != null) {
            View actionView = mNotificationsMenuItem.getActionView();
            mNotificationsIndicator = actionView.findViewById(R.id.notifications_indicator);
            updateNotificationIndicator(mSelectedFactoryId);
        }

        return mSelectedFactoryId;
    }

    @Override
    protected int[] getRightNavigationDrawerMenuResources() {
        return mFactory.getToolDrawerMenuResIds();
    }

    @Override
    protected int getInitialRightDrawerSelection() {
        return mFactory.getInitialToolDrawerSelection();
    }

    @Override
    protected void onPrepareRightNavigationDrawerMenu(Menu menu) {
        super.onPrepareRightNavigationDrawerMenu(menu);
        mFactory.prepareToolDrawerMenu(menu);
    }

    @Override
    protected void configureLeftDrawerHeader(View header) {
        super.configureLeftDrawerHeader(header);

        mAvatarView = header.findViewById(R.id.avatar);
        mUserExtraView = header.findViewById(R.id.user_extra);

        TextView userNameView = header.findViewById(R.id.user_name);
        userNameView.setText(mUserLogin);

        updateUserInfo();

        mDrawerSwitcher = header.findViewById(R.id.switcher);
        mDrawerSwitcher.setVisibility(View.VISIBLE);

        mDrawerSwitcher.setOnClickListener(this);

        View clickableBackground = header.findViewById(R.id.drawer_header);
        clickableBackground.setOnClickListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        super.onNavigationItemSelected(item);

        updateNotificationIndicator(item.getItemId());

        if (mFactory != null && mFactory.onDrawerItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();
        FragmentFactory factory = getFactoryForItem(id);

        if (factory != null) {
            switchTo(id, factory);
            return true;
        }

        switch (id) {
            case R.id.profile:
                startActivity(UserActivity.makeIntent(this, mUserLogin));
                updateDrawerMode(false);
                return true;
            case R.id.logout:
                Gh4Application.get().logout();
                goToToplevelActivity();
                finish();
                return true;
            case R.id.add_account:
                new BrowserLogoutDialogFragment().show(getSupportFragmentManager(), "browserlogout");
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
        }

        int accountCount = Gh4Application.get().getAccounts().size();
        if (id >= OTHER_ACCOUNTS_GROUP_BASE_ID && id < OTHER_ACCOUNTS_GROUP_BASE_ID + accountCount) {
            switchActiveUser(item.getTitle().toString());
            return true;
        }

        return false;
    }

    @Override
    protected void onDrawerClosed(boolean right) {
        super.onDrawerClosed(right);
        if (!right) {
            updateDrawerMode(false);
        }
    }

    private void switchActiveUser(String login) {
        Gh4Application.get().setActiveLogin(login);
        mUserLogin = login;
        onRefresh();
        closeDrawers();
        switchTo(mSelectedFactoryId, getFactoryForItem(mSelectedFactoryId));
        recreate();
    }

    private FragmentFactory getFactoryForItem(int id) {
        switch (id) {
            case R.id.news_feed:
                return new NewsFeedFactory(this, mUserLogin);
            case R.id.notifications:
                return new NotificationListFactory(this);
            case R.id.my_repos:
                return new RepositoryFactory(this, mUserLogin, getPrefs());
            case R.id.my_issues:
                return new IssueListFactory(this, mUserLogin, false);
            case R.id.my_prs:
                return new IssueListFactory(this, mUserLogin, true);
            case R.id.my_gists:
                return new GistFactory(this, mUserLogin);
            case R.id.search:
                return new SearchFactory(this);
            case R.id.bookmarks:
                return new BookmarkFactory(this);
            case R.id.pub_timeline:
                return new TimelineFactory(this);
            case R.id.blog:
                return new BlogFactory(this);
            case R.id.trend:
                return new TrendingFactory(this);
        }
        return null;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mFactory.getTabTitleResIds();
    }

    @Override
    protected int[] getHeaderColorAttrs() {
        return mFactory.getHeaderColorAttrs();
    }

    @Override
    protected Fragment makeFragment(int position) {
        return mFactory.makeFragment(position);
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        mFactory.onFragmentInstantiated(f, position);
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        mFactory.onFragmentDestroyed(f);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mFactory.onCreateOptionsMenu(menu)) {
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mFactory.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS) {
            if (data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_THEME_CHANGED, false)) {
                goToToplevelActivity();
                finish();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected Intent navigateUp() {
        return getToplevelActivityIntent();
    }

    @Override
    public void onRefresh() {
        forceLoaderReload(0, 1);
        mFactory.onRefresh();
        super.onRefresh();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        //noinspection StatementWithEmptyBody
        if (mFactory instanceof RepositoryFactory) {
            // happens when load is done; we ignore it as we don't want to close the IME in that case
        } else {
            super.supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (!closeDrawers() && fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void initiateFilter() {
        toggleRightSideDrawer();
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment object) {
        return true;
    }

    public void doInvalidateOptionsMenuAndToolDrawer() {
        super.supportInvalidateOptionsMenu();
        updateRightNavigationDrawer();
    }

    @Override
    public void invalidateTabs() {
        super.invalidateTabs();
    }

    @Override
    public void invalidateFragments() {
        super.invalidateFragments();
    }

    public void toggleToolDrawer() {
        toggleRightSideDrawer();
    }

    public void invalidateTitle() {
        getSupportActionBar().setTitle(mFactory.getTitleResId());
    }

    private int determineInitialPage() {
        String initialPage = getIntent().hasExtra("initial_page")
                ? getIntent().getStringExtra("initial_page")
                : getPrefs().getString(SettingsFragment.KEY_START_PAGE, "newsfeed");
        if (TextUtils.equals(initialPage, "last")) {
            initialPage = getPrefs().getString("last_selected_home_page", "newsfeed");
        }
        for (int i = 0; i < START_PAGE_MAPPING.size(); i++) {
            if (TextUtils.equals(initialPage, START_PAGE_MAPPING.valueAt(i))) {
                return START_PAGE_MAPPING.keyAt(i);
            }
        }
        return R.id.news_feed;
    }

    private void updateUserInfo() {
        if (mUserInfo == null) {
            mAvatarView.setImageDrawable(new AvatarHandler.DefaultAvatarDrawable(mUserLogin, null));
            return;
        }
        if (mAvatarView != null) {
            AvatarHandler.assignAvatar(mAvatarView, mUserInfo);
        }
        if (mUserExtraView != null) {
            if (TextUtils.isEmpty(mUserInfo.name())) {
                mUserExtraView.setVisibility(View.GONE);
            } else {
                mUserExtraView.setText(mUserInfo.name());
                mUserExtraView.setVisibility(View.VISIBLE);
            }
        }
        mFactory.setUserInfo(mUserInfo);
    }

    private void updateDrawerMode(boolean accountMode) {
        mLeftDrawerMenu.setGroupVisible(R.id.my_items, !accountMode);
        mLeftDrawerMenu.setGroupVisible(R.id.navigation, !accountMode);
        mLeftDrawerMenu.setGroupVisible(R.id.explore, !accountMode);
        mLeftDrawerMenu.setGroupVisible(R.id.settings, !accountMode);
        mLeftDrawerMenu.setGroupVisible(R.id.account, accountMode);
        mLeftDrawerMenu.setGroupVisible(R.id.other_accounts, accountMode);

        if (accountMode) {
            // repopulate other account list
            for (int i = 0; ; i++) {
                MenuItem item = mLeftDrawerMenu.findItem(OTHER_ACCOUNTS_GROUP_BASE_ID + i);
                if (item == null) {
                    break;
                }
                mLeftDrawerMenu.removeItem(item.getItemId());
            }

            int id = OTHER_ACCOUNTS_GROUP_BASE_ID;
            SparseArray<String> accounts = Gh4Application.get().getAccounts();
            for (int i = 0; i < accounts.size(); i++) {
                String login = accounts.valueAt(i);
                if (ApiHelpers.loginEquals(mUserLogin, login)) {
                    continue;
                }

                MenuItem item = mLeftDrawerMenu.add(R.id.other_accounts, id++, Menu.NONE, login);
                AvatarHandler.assignAvatar(this, item, login, accounts.keyAt(i), null);
            }
        }

        mDrawerSwitcher.setImageResource(accountMode
                ? R.drawable.drop_up_arrow_white : R.drawable.drop_down_arrow_white);
        mDrawerInAccountMode = accountMode;
    }

    private void switchTo(int itemId, FragmentFactory factory) {
        if (mFactory != null) {
            mFactory.onDestroy();
        }
        mFactory = factory;
        mSelectedFactoryId = itemId;
        mFactory.setUserInfo(mUserInfo);

        getPrefs().edit()
                .putString("last_selected_home_page", START_PAGE_MAPPING.get(mSelectedFactoryId))
                .apply();

        updateRightNavigationDrawer();
        super.supportInvalidateOptionsMenu();
        getSupportFragmentManager().popBackStackImmediate(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        invalidateTitle();
        invalidateTabs();
    }

    public static class BrowserLogoutDialogFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.browser_logout_dialog_title)
                    .setMessage(R.string.browser_logout_dialog_text)
                    .setPositiveButton(R.string.go_to_logout_page, this)
                    .setNeutralButton(R.string.continue_login, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (which == DialogInterface.BUTTON_NEUTRAL) {
                Github4AndroidActivity.launchLogin(getActivity());
            } else if (which == DialogInterface.BUTTON_POSITIVE) {
                Uri uri = Uri.parse("https://github.com/logout");
                FragmentManager fm = getActivity().getSupportFragmentManager();
                IntentUtils.openInCustomTabOrBrowser(getActivity(), uri);
                new BrowserLogoutCompletedDialogFragment().show(fm, "browserlogoutcomplete");
            }
        }
    }

    public static class BrowserLogoutCompletedDialogFragment extends DialogFragment implements
            DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.browser_logout_completed_dialog_text)
                    .setPositiveButton(R.string.continue_login, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Github4AndroidActivity.launchLogin(getActivity());
        }
    }
}
