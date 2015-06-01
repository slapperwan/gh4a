package com.gh4a.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.gh4a.BasePagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.activities.BookmarkListActivity;
import com.gh4a.activities.SearchActivity;
import com.gh4a.activities.SettingsActivity;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.RepositoryListContainerFragment;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.UserLoader;
import com.gh4a.utils.AvatarHandler;
import com.gh4a.utils.IntentUtils;

import org.eclipse.egit.github.core.User;

import java.util.Arrays;
import java.util.List;

public class HomeActivity extends BasePagerActivity implements
        View.OnClickListener, RepositoryListContainerFragment.Callback {
    private static final int REQUEST_SETTINGS = 10000;

    private FragmentFactory mFactory;
    private SparseArray<Fragment> mFragments;
    private DrawerAdapter mDrawerAdapter;
    private ImageView mAvatarView;
    private String mUserLogin;
    private int mSelectedFactoryId;
    private boolean mStarted;

    private static final String STATE_KEY_FACTORY_ITEM = "factoryItem";

    private static final int ITEM_NEWS_FEED = 1;
    private static final int ITEM_REPOSITORIES = 2;
    private static final int ITEM_ISSUES = 3;
    private static final int ITEM_PULLREQUESTS = 4;
    private static final int ITEM_GISTS = 5;
    private static final int ITEM_TIMELINE = 6;
    private static final int ITEM_TRENDING = 7;
    private static final int ITEM_BLOG = 8;
    private static final int ITEM_SEARCH = 9;
    private static final int ITEM_BOOKMARKS = 10;
    private static final int ITEM_SETTINGS = 11;

    private static final List<DrawerAdapter.Item> DRAWER_ITEMS = Arrays.asList(
        new DrawerAdapter.EntryItem(R.string.user_news_feed, 0, ITEM_NEWS_FEED),
        new DrawerAdapter.EntryItem(R.string.my_repositories, 0, ITEM_REPOSITORIES),
        new DrawerAdapter.EntryItem(R.string.my_issues, 0, ITEM_ISSUES),
        new DrawerAdapter.EntryItem(R.string.my_pull_requests, 0, ITEM_PULLREQUESTS),
        new DrawerAdapter.EntryItem(R.string.my_gists, 0, ITEM_GISTS),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.EntryItem(R.string.pub_timeline, 0, ITEM_TIMELINE),
        new DrawerAdapter.EntryItem(R.string.trend, 0, ITEM_TRENDING),
        new DrawerAdapter.EntryItem(R.string.blog, 0, ITEM_BLOG),
        new DrawerAdapter.DividerItem(),
        new DrawerAdapter.EntryItem(R.string.search, 0, ITEM_SEARCH),
        new DrawerAdapter.EntryItem(R.string.bookmarks, 0, ITEM_BOOKMARKS),
        new DrawerAdapter.EntryItem(R.string.settings, 0, ITEM_SETTINGS)
    );

    private LoaderCallbacks<User> mUserCallback = new LoaderCallbacks<User>() {
        @Override
        public Loader<LoaderResult<User>> onCreateLoader(int id, Bundle args) {
            return new UserLoader(HomeActivity.this, mUserLogin);
        }
        @Override
        public void onResultReady(LoaderResult<User> result) {
            User user = result.getData();
            mAvatarView.setTag(user);
            AvatarHandler.assignAvatar(mAvatarView, user);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mUserLogin = Gh4Application.get().getAuthLogin();
        if (savedInstanceState != null) {
            mSelectedFactoryId = savedInstanceState.getInt(STATE_KEY_FACTORY_ITEM);
        } else {
            mSelectedFactoryId = ITEM_NEWS_FEED;
        }
        mFactory = getFactoryForItem(mSelectedFactoryId, savedInstanceState);
        mFragments = new SparseArray<>();

        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

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
    public void onClick(View view) {
        User user = (User) view.getTag();
        Intent intent = IntentUtils.getUserActivityIntent(this, user);
        if (intent != null) {
            closeDrawers();
            startActivity(intent);
        }
    }

    @Override
    protected ListAdapter getLeftNavigationDrawerAdapter() {
        mDrawerAdapter = new DrawerAdapter(this, DRAWER_ITEMS);
        return mDrawerAdapter;
    }

    @Override
    protected ListAdapter getRightNavigationDrawerAdapter() {
        return mFactory.getToolDrawerAdapter();
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
    protected boolean onDrawerItemSelected(boolean left, int position) {
        if (!left) {
            return mFactory.onDrawerItemSelected(position);
        }

        int id = DRAWER_ITEMS.get(position).getId();
        FragmentFactory factory = getFactoryForItem(id, null);

        if (factory != null) {
            switchTo(id, factory);
            return true;
        }

        switch (id) {
            case ITEM_SEARCH:
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            case ITEM_BOOKMARKS:
                startActivity(new Intent(this, BookmarkListActivity.class));
                return true;
            case ITEM_SETTINGS:
                startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_SETTINGS);
                return true;
        }

        return super.onDrawerItemSelected(left, position);
    }

    private FragmentFactory getFactoryForItem(int id, Bundle savedInstanceState) {
        switch (id) {
            case ITEM_NEWS_FEED:
                return new NewsFeedFactory(this, mUserLogin);
            case ITEM_REPOSITORIES:
                return new RepositoryFactory(this, mUserLogin, savedInstanceState);
            case ITEM_ISSUES:
                return new IssueListFactory(this, mUserLogin, false);
            case ITEM_PULLREQUESTS:
                return new IssueListFactory(this, mUserLogin, true);
            case ITEM_GISTS:
                return new GistFactory(this, mUserLogin);
            case ITEM_TIMELINE:
                return new TimelineFactory(this);
            case ITEM_BLOG:
                return new BlogFactory(this);
            case ITEM_TRENDING:
                return new TrendingFactory(this);
        }
        return null;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mFactory.getTabTitleResIds();
    }

    @Override
    protected Fragment getFragment(int position) {
        Fragment fragment = mFactory.getFragment(position);
        mFragments.put(position, fragment);
        return fragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mDrawerAdapter != null) {
            updateDrawerSelectionState();
        }
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
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        for (int i = 0; i < mFragments.size(); i++) {
            int position = mFragments.keyAt(i);
            mFactory.onRefreshFragment(mFragments.get(position));
        }
        refreshDone();
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

    public void doInvalidateOptionsMenu() {
        super.supportInvalidateOptionsMenu();
    }

    public void invalidateFragments() {
        super.invalidateFragments();
        mFragments.clear();
    }

    public void toggleToolDrawer() {
        toggleRightSideDrawer();
    }

    public void invalidateTitle() {
        getSupportActionBar().setTitle(mFactory.getTitleResId());
    }

    private void switchTo(int itemId, FragmentFactory factory) {
        mFactory = factory;
        mSelectedFactoryId = itemId;

        updateRightNavigationDrawer();
        updateDrawerSelectionState();
        super.supportInvalidateOptionsMenu();
        getSupportFragmentManager().popBackStackImmediate(null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE);
        invalidateTitle();
        invalidateTabs();
        if (mStarted) {
            mFactory.onStart();
        }
    }

    private void updateDrawerSelectionState() {
        for (int i = 0; i < mDrawerAdapter.getCount(); i++) {
            Object item = mDrawerAdapter.getItem(i);
            if (item instanceof DrawerAdapter.EntryItem) {
                DrawerAdapter.EntryItem dei = (DrawerAdapter.EntryItem) item;
                dei.setSelected(dei.getId() == mSelectedFactoryId);
            }
        }
        mDrawerAdapter.notifyDataSetChanged();
    }
}
