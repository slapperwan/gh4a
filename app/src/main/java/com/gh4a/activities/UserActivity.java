package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.BackgroundTask;
import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.PublicEventListFragment;
import com.gh4a.fragment.UserFragment;
import com.gh4a.loader.IsFollowingUserLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;

import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.UserService;

public class UserActivity extends BasePagerActivity {
    public static Intent makeIntent(Context context, String login) {
        return makeIntent(context, login, null);
    }

    public static Intent makeIntent(Context context, User user) {
        if (user == null) {
            return null;
        }
        return makeIntent(context, user.getLogin(), user.getName());
    }

    public static Intent makeIntent(Context context, String login, String name) {
        if (login == null) {
            return null;
        }
        return new Intent(context, UserActivity.class)
                .putExtra("login", login)
                .putExtra("name", name);
    }

    private String mUserLogin;
    private String mUserName;
    private boolean mIsSelf;
    private UserFragment mUserFragment;
    private Boolean mIsFollowing;

    private static final int[] TAB_TITLES = new int[] {
        R.string.about, R.string.user_public_activity
    };

    private final LoaderCallbacks<Boolean> mIsFollowingCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsFollowingUserLoader(UserActivity.this, mUserLogin);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsFollowing = result;
            supportInvalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);
        if (!mIsSelf && Gh4Application.get().isAuthorized()) {
            getSupportLoaderManager().initLoader(4, null, mIsFollowingCallback);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString("login");
        mUserName = extras.getString("name");
        mIsSelf = ApiHelpers.loginEquals(mUserLogin, Gh4Application.get().getAuthLogin());
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TAB_TITLES;
    }

    @Override
    protected Fragment makeFragment(int position) {
        switch (position) {
            case 0: return UserFragment.newInstance(mUserLogin);
            case 1: return PublicEventListFragment.newInstance(mUserLogin);
        }
        return null;
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        if (position == 0) {
            mUserFragment = (UserFragment) f;
        }
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mUserFragment) {
            mUserFragment = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem followAction = menu.findItem(R.id.follow);
        if (followAction != null) {
            if (!mIsSelf && Gh4Application.get().isAuthorized()) {
                followAction.setVisible(true);
                if (mIsFollowing == null) {
                    MenuItemCompat.setActionView(followAction, R.layout.ab_loading);
                    MenuItemCompat.expandActionView(followAction);
                } else if (mIsFollowing) {
                    followAction.setTitle(R.string.user_unfollow_action);
                } else {
                    followAction.setTitle(R.string.user_follow_action);
                }
            } else {
                followAction.setVisible(false);
            }
        }

        MenuItem bookmarkAction = menu.findItem(R.id.bookmark);
        if (bookmarkAction != null) {
            String url = "https://github.com/" + mUserLogin;
            bookmarkAction.setTitle(BookmarksProvider.hasBookmarked(this, url)
                    ? R.string.remove_bookmark
                    : R.string.bookmark_user);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return getToplevelActivityIntent();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String url = "https://github.com/" + mUserLogin;
        switch (item.getItemId()) {
            case R.id.follow:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                new UpdateFollowTask().schedule();
                return true;
            case R.id.share:
                int subjectId = StringUtils.isBlank(mUserName)
                        ? R.string.share_user_subject_loginonly : R.string.share_user_subject;
                IntentUtils.share(this, getString(subjectId, mUserLogin, mUserName), url);
                return true;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(url));
                return true;
            case R.id.bookmark:
                if (BookmarksProvider.hasBookmarked(this, url)) {
                    BookmarksProvider.removeBookmark(this, url);
                } else {
                    BookmarksProvider.saveBookmark(this, mUserLogin,
                            BookmarksProvider.Columns.TYPE_USER, url, mUserName, true);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRefresh() {
        if (forceLoaderReload(4)) {
            mIsFollowing = null;
            supportInvalidateOptionsMenu();
        }
        super.onRefresh();
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