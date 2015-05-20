package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.gh4a.BackgroundTask;
import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.PrivateEventListFragment;
import com.gh4a.fragment.PublicEventListFragment;
import com.gh4a.fragment.UserFragment;
import com.gh4a.loader.IsFollowingUserLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.service.UserService;

import java.util.Arrays;
import java.util.List;

public class UserActivity extends BasePagerActivity {
    private static final int REQUEST_SETTINGS = 10000;
    public static final String EXTRA_TOPLEVEL_MODE = "toplevel_mode";

    private String mUserLogin;
    private String mUserName;
    private boolean mIsTopLevelMode;
    private boolean mIsSelf;
    private UserFragment mUserFragment;
    private PrivateEventListFragment mPrivateEventListFragment;
    private PublicEventListFragment mPublicEventListFragment;
    private Boolean mIsFollowing;

    private static final int[] TITLES_SELF = new int[] {
        R.string.about, R.string.user_news_feed,
        R.string.user_your_actions
    };
    private static final int[] TITLES_OTHER = new int[] {
        R.string.about, R.string.user_public_activity
    };

    private static final int ITEM_ISSUES = 1;
    private static final int ITEM_PULLREQUESTS = 2;
    private static final int ITEM_SEARCH = 3;
    private static final int ITEM_BOOKMARKS = 4;
    private static final int ITEM_SETTINGS = 5;
    private static final int ITEM_TIMELINE = 6;
    private static final int ITEM_TRENDING = 7;
    private static final int ITEM_BLOG = 8;
    private static final List<DrawerAdapter.Item> DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.SectionHeaderItem(R.string.navigation),
        new DrawerAdapter.EntryItem(R.string.my_issues, 0, ITEM_ISSUES),
        new DrawerAdapter.EntryItem(R.string.my_pull_requests, 0, ITEM_PULLREQUESTS),
        new DrawerAdapter.EntryItem(R.string.search, 0, ITEM_SEARCH),
        new DrawerAdapter.EntryItem(R.string.bookmarks, 0, ITEM_BOOKMARKS),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.SectionHeaderItem(R.string.explore),
        new DrawerAdapter.EntryItem(R.string.pub_timeline, 0, ITEM_TIMELINE),
        new DrawerAdapter.EntryItem(R.string.trend, 0, ITEM_TRENDING),
        new DrawerAdapter.EntryItem(R.string.blog, 0, ITEM_BLOG),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.EntryItem(R.string.settings, 0, ITEM_SETTINGS)
    );

    private LoaderCallbacks<Boolean> mIsFollowingCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsFollowingUserLoader(UserActivity.this, mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            mIsFollowing = result.getData();
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserName = data.getString(Constants.User.NAME);
        mIsSelf = mUserLogin.equals(Gh4Application.get().getAuthLogin());
        mIsTopLevelMode = mIsSelf && data.getBoolean(EXTRA_TOPLEVEL_MODE, false);

        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (mIsTopLevelMode) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            actionBar.setTitle(mUserLogin);
            actionBar.setDisplayHomeAsUpEnabled(true);
            if (Gh4Application.get().isAuthorized()) {
                getSupportLoaderManager().initLoader(4, null, mIsFollowingCallback);
            }
        }
    }

    @Override
    protected ListAdapter getNavigationDrawerAdapter() {
        if (mIsTopLevelMode) {
            return new DrawerAdapter(this, DRAWER_ITEMS);
        }
        return super.getNavigationDrawerAdapter();
    }

    @Override
    protected boolean onDrawerItemSelected(int position) {
        switch (DRAWER_ITEMS.get(position).getId()) {
            case ITEM_SETTINGS:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
            case ITEM_SEARCH:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case ITEM_BOOKMARKS:
                startActivity(new Intent(this, BookmarkListActivity.class));
                return true;
            case ITEM_TIMELINE:
                startActivity(new Intent(this, TimelineActivity.class));
                return true;
            case ITEM_BLOG:
                startActivity(new Intent(this, BlogListActivity.class));
                return true;
            case ITEM_TRENDING:
                startActivity(new Intent(this, TrendingActivity.class));
                return true;
            case ITEM_ISSUES:
                Intent intent = new Intent(this, IssueListMineActivity.class);
                intent.putExtra("type", "issue");
                startActivity(intent);
                return true;
            case ITEM_PULLREQUESTS:
                intent = new Intent(this, IssueListMineActivity.class);
                intent.putExtra("type", "pr");
                startActivity(intent);
                return true;
        }
        return super.onDrawerItemSelected(position);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mIsTopLevelMode ? TITLES_SELF : TITLES_OTHER;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0:
                mUserFragment = UserFragment.newInstance(mUserLogin, mUserName);
                return mUserFragment;
            case 1:
                if (mIsTopLevelMode) {
                    mPrivateEventListFragment =
                            PrivateEventListFragment.newInstance(mUserLogin);
                    return mPrivateEventListFragment;
                }
                // else fall through: in non-toplevel mode, the public
                // action fragment is at index 1
            case 2:
                mPublicEventListFragment =
                        PublicEventListFragment.newInstance(mUserLogin);
                return mPublicEventListFragment;
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mIsTopLevelMode) {
            return false;
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem followAction = menu.findItem(R.id.follow);
        if (followAction != null) {
            followAction.setVisible(!mIsSelf && Gh4Application.get().isAuthorized());
            if (followAction.isVisible()) {
                if (mIsFollowing == null) {
                    MenuItemCompat.setActionView(followAction, R.layout.ab_loading);
                    MenuItemCompat.expandActionView(followAction);
                } else if (mIsFollowing) {
                    followAction.setTitle(R.string.user_unfollow_action);
                } else {
                    followAction.setTitle(R.string.user_follow_action);
                }
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SETTINGS) {
            if (data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_THEME_CHANGED, false)
                    || data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_AUTH_CHANGED, false)) {
                goToToplevelActivity(false);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.follow:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                AsyncTaskCompat.executeParallel(new UpdateFollowTask());
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                int subjectId = StringUtils.isBlank(mUserName)
                        ? R.string.share_user_subject_loginonly : R.string.share_user_subject;
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, getString(subjectId, mUserLogin, mUserName));
                shareIntent.putExtra(Intent.EXTRA_TEXT,  "https://github.com/" + mUserLogin);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.bookmark:
                Intent bookmarkIntent = new Intent(this, getClass());
                bookmarkIntent.putExtra(Constants.User.LOGIN, mUserLogin);
                bookmarkIntent.putExtra(Constants.User.NAME, mUserName);
                saveBookmark(mUserLogin, BookmarksProvider.Columns.TYPE_USER, bookmarkIntent, mUserName);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        if (mUserFragment != null) {
            mUserFragment.refresh();
        }
        if (mPrivateEventListFragment != null) {
            mPrivateEventListFragment.refresh();
        }
        if (mPublicEventListFragment != null) {
            mPublicEventListFragment.refresh();
        }
        refreshDone();
    }

    private class UpdateFollowTask extends BackgroundTask<Void> {
        public UpdateFollowTask() {
            super(UserActivity.this);
        }

        @Override
        protected Void run() throws Exception {
            UserService userService = (UserService)
                    Gh4Application.get().getService(Gh4Application.USER_SERVICE);
            if (mIsFollowing) {
                userService.unfollow(mUserLogin);
            } else {
                userService.follow(mUserLogin);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mIsFollowing = !mIsFollowing;
            if (mUserFragment != null) {
                mUserFragment.updateFollowingAction(mIsFollowing);
            }
            supportInvalidateOptionsMenu();
        }
    }
}