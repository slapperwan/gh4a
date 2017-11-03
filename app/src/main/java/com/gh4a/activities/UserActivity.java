package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.PublicEventListFragment;
import com.gh4a.fragment.UserFragment;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.meisolsson.githubsdk.model.User;

public class UserActivity extends BaseFragmentPagerActivity {
    public static Intent makeIntent(Context context, String login) {
        return makeIntent(context, login, null);
    }

    public static Intent makeIntent(Context context, User user) {
        if (user == null) {
            return null;
        }
        return makeIntent(context, user.login(), user.name());
    }

    private static Intent makeIntent(Context context, String login, String name) {
        if (login == null) {
            return null;
        }
        return new Intent(context, UserActivity.class)
                .putExtra("login", login)
                .putExtra("name", name);
    }

    private String mUserLogin;
    private String mUserName;
    private UserFragment mUserFragment;

    private static final int[] TAB_TITLES = new int[] {
        R.string.about, R.string.user_public_activity
    };

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return mUserLogin;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mUserLogin = extras.getString("login");
        mUserName = extras.getString("name");
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
    public boolean displayDetachAction() {
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem bookmarkAction = menu.findItem(R.id.bookmark);
        if (bookmarkAction != null) {
            String url = "https://github.com/" + mUserLogin;
            bookmarkAction.setTitle(BookmarksProvider.hasBookmarked(this, url)
                    ? R.string.remove_bookmark
                    : R.string.bookmark);
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
}