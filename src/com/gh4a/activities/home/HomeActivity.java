package com.gh4a.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.BookmarkListActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.SettingsActivity;
import com.gh4a.fragment.RepositoryListContainerFragment;
import com.gh4a.fragment.SettingsFragment;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.User;

public class HomeActivity extends BasePagerActivity implements
        View.OnClickListener, RepositoryListContainerFragment.Callback {
    private static final int REQUEST_SETTINGS = 10000;

    private FragmentFactory mFactory;
    private ImageView mAvatarView;
    private String mUserLogin;
    private int mSelectedFactoryId;
    private boolean mStarted;

    private static final String STATE_KEY_FACTORY_ITEM = "factoryItem";

    private static final SparseArray<String> START_PAGE_MAPPING = new SparseArray<>();
    static {
        START_PAGE_MAPPING.put(R.id.news_feed, "newsfeed");
        START_PAGE_MAPPING.put(R.id.my_repos, "repos");
        START_PAGE_MAPPING.put(R.id.my_issues, "issues");
        START_PAGE_MAPPING.put(R.id.my_prs, "prs");
        START_PAGE_MAPPING.put(R.id.my_gists, "gists");
        START_PAGE_MAPPING.put(R.id.pub_timeline, "timeline");
        START_PAGE_MAPPING.put(R.id.trend, "trends");
        START_PAGE_MAPPING.put(R.id.blog, "blog");
    }

    private LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>(this) {
        @Override
        protected Loader<LoaderResult<User>> onCreateLoader() {
            return new UserLoader(HomeActivity.this, mUserLogin);
        }
        @Override
        protected void onResultReady(User result) {
            mAvatarView.setTag(result);
            AvatarHandler.assignAvatar(mAvatarView, result);
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

        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mFactory.getTitleResId());
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
        User user = (User) view.getTag();
        Intent intent = IntentUtils.getUserActivityIntent(this, user);
        if (intent != null) {
            closeDrawers();
            startActivity(intent);
        }
    }

    @Override
    protected int getLeftNavigationDrawerMenuResource() {
        return R.menu.home_nav_drawer;
    }

    @Override
    protected int[] getRightNavigationDrawerMenuResources() {
        return mFactory.getToolDrawerMenuResIds();
    }

    @Override
    protected void onPrepareRightNavigationDrawerMenu(Menu menu) {
        super.onPrepareRightNavigationDrawerMenu(menu);
        mFactory.prepareToolDrawerMenu(menu);
    }

    @Override
    protected View getLeftDrawerTitle(ViewGroup container) {
        View view = getLayoutInflater().inflate(R.layout.drawer_title_home, container, false);
        mAvatarView = (ImageView) view.findViewById(R.id.avatar);
        mAvatarView.setOnClickListener(this);

        getSupportLoaderManager().initLoader(0, null, mUserCallback);

        TextView nameView = (TextView) view.findViewById(R.id.user_name);
        nameView.setText(mUserLogin);

        return view;
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        super.onNavigationItemSelected(item);

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
            case R.id.search:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case R.id.bookmarks:
                startActivity(new Intent(this, BookmarkListActivity.class));
                return true;
            case R.id.settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
        }

        return false;
    }

    private FragmentFactory getFactoryForItem(int id) {
        switch (id) {
            case R.id.news_feed:
                return new NewsFeedFactory(this, mUserLogin);
            case R.id.my_repos:
                return new RepositoryFactory(this, mUserLogin, getPrefs());
            case R.id.my_issues:
                return new IssueListFactory(this, mUserLogin, false);
            case R.id.my_prs:
                return new IssueListFactory(this, mUserLogin, true);
            case R.id.my_gists:
                return new GistFactory(this, mUserLogin);
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
    protected int[][] getTabHeaderColors() {
        return mFactory.getTabHeaderColors();
    }

    @Override
    protected int[] getHeaderColors() {
        return mFactory.getHeaderColors();
    }

    @Override
    protected Fragment getFragment(int position) {
        return mFactory.getFragment(position);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mStarted = true;
        mFactory.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mStarted = false;
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
            if (data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_THEME_CHANGED, false)
                    || data.getBooleanExtra(SettingsActivity.RESULT_EXTRA_AUTH_CHANGED, false)) {
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
        getSupportLoaderManager().getLoader(0).onContentChanged();
        super.onRefresh();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        if (mFactory instanceof RepositoryFactory) {
            // happens when load is done; we ignore it as we don't want to close the IME in that case
        } else {
            super.supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else if (!closeDrawers()) {
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
        String initialPage = getPrefs().getString(SettingsFragment.KEY_START_PAGE, "newsfeed");
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

    private void switchTo(int itemId, FragmentFactory factory) {
        if (mFactory != null) {
            mFactory.onDestroy();
        }
        mFactory = factory;
        mSelectedFactoryId = itemId;

        getPrefs().edit()
                .putString("last_selected_home_page", START_PAGE_MAPPING.get(mSelectedFactoryId))
                .apply();

        updateRightNavigationDrawer();
        super.supportInvalidateOptionsMenu();
        getSupportFragmentManager().popBackStackImmediate(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        invalidateTitle();
        invalidateTabs();
        if (mStarted) {
            mFactory.onStart();
        }
    }
}
