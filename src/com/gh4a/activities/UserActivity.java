package com.gh4a.activities;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.egit.github.core.service.UserService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.BackgroundTask;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.PrivateEventListFragment;
import com.gh4a.fragment.PublicEventListFragment;
import com.gh4a.fragment.RepositoryIssueListFragment;
import com.gh4a.fragment.UserFragment;
import com.gh4a.loader.IsFollowingUserLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.utils.StringUtils;

public class UserActivity extends LoadingFragmentPagerActivity {
    private String mUserLogin;
    private String mUserName;
    private boolean mIsLoginUserPage;
    private UserFragment mUserFragment;
    private PrivateEventListFragment mPrivateEventListFragment;
    private PublicEventListFragment mPublicEventListFragment;
    private RepositoryIssueListFragment mRepositoryIssueListFragment;
    public Boolean mIsFollowing;

    private static final int[] TITLES_SELF = new int[] {
        R.string.about, R.string.user_news_feed,
        R.string.user_your_actions, R.string.issues
    };
    private static final int[] TITLES_OTHER = new int[] {
        R.string.about, R.string.user_public_activity
    };

    private LoaderCallbacks<Boolean> mIsFollowingCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsFollowingUserLoader(UserActivity.this, mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            mIsFollowing = result.getData();
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserName = data.getString(Constants.User.NAME);
        mIsLoginUserPage = mUserLogin.equals(Gh4Application.get(this).getAuthLogin());

        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (mIsLoginUserPage) {
            actionBar.setDisplayShowHomeEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        } else {
            actionBar.setTitle(mUserLogin);
            actionBar.setDisplayHomeAsUpEnabled(true);
            getSupportLoaderManager().initLoader(4, null, mIsFollowingCallback);
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mIsLoginUserPage ? TITLES_SELF : TITLES_OTHER;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0:
                mUserFragment = UserFragment.newInstance(mUserLogin, mUserName);
                return mUserFragment;
            case 1:
                mPrivateEventListFragment =
                        PrivateEventListFragment.newInstance(mUserLogin, mIsLoginUserPage);
                return mPrivateEventListFragment;
            case 2:
                mPublicEventListFragment =
                        PublicEventListFragment.newInstance(mUserLogin, false);
                return mPublicEventListFragment;
            case 3:
                Map<String, String> filterData = new HashMap<String, String>();
                filterData.put("filter", "subscribed");
                mRepositoryIssueListFragment = RepositoryIssueListFragment.newInstance(filterData);
                return mRepositoryIssueListFragment;
        }
        return null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean authorized = Gh4Application.get(this).isAuthorized();

        MenuItem logoutAction = menu.findItem(R.id.logout);
        logoutAction.setTitle(authorized ? R.string.logout : R.string.login);
        logoutAction.setVisible(mIsLoginUserPage || !authorized);

        MenuItem followAction = menu.findItem(R.id.follow);
        followAction.setVisible(!mIsLoginUserPage && authorized);
        if (followAction.isVisible()) {
            if (mIsFollowing == null) {
                followAction.setActionView(R.layout.ab_loading);
                followAction.expandActionView();
            } else if (mIsFollowing) {
                followAction.setTitle(R.string.user_unfollow_action);
            } else {
                followAction.setTitle(R.string.user_follow_action);
            }
        }

        menu.findItem(R.id.bookmarks).setVisible(mIsLoginUserPage);
        menu.findItem(R.id.share).setVisible(!mIsLoginUserPage);
        menu.findItem(R.id.bookmark).setVisible(!mIsLoginUserPage);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        goToToplevelActivity(0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                if (mUserFragment != null) {
                    mUserFragment.refresh();
                }
                if (mPrivateEventListFragment != null) {
                    mPrivateEventListFragment.refresh();
                }
                if (mPublicEventListFragment != null) {
                    mPublicEventListFragment.refresh();
                }
                if (mRepositoryIssueListFragment != null) {
                    mRepositoryIssueListFragment.refresh();
                }
                return true;
            case R.id.dark:
                setThemeSetting(Constants.Theme.DARK);
                return true;
            case R.id.light:
                setThemeSetting(Constants.Theme.LIGHT);
                return true;
            case R.id.lightDark:
                setThemeSetting(Constants.Theme.LIGHTDARK);
                return true;
            case R.id.follow:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                new UpdateFollowTask().execute();
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
        boolean result = super.onOptionsItemSelected(item);
        if (item.getItemId() == R.id.logout) {
            finish();
        }
        return result;
    }

    @SuppressLint("NewApi")
    private void setThemeSetting(int theme) {
        Gh4Application.get(this).setThemeSetting(theme);
        recreate();
    }

    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            final Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            startActivity(intent);
            overridePendingTransition(0, 0);

            finish();
            overridePendingTransition(0, 0);
        }
    }

    private class UpdateFollowTask extends BackgroundTask<Void> {
        public UpdateFollowTask() {
            super(UserActivity.this);
        }

        @Override
        protected Void run() throws Exception {
            UserService userService = (UserService)
                    Gh4Application.get(mContext).getService(Gh4Application.USER_SERVICE);
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
            invalidateOptionsMenu();
        }
    }
}