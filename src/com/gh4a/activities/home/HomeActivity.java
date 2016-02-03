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
    private ImageView mAvatarView;
    private String mUserLogin;
    private int mSelectedFactoryId;
    private boolean mStarted;

    private static final String STATE_KEY_FACTORY_ITEM = "factoryItem";

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
            mSelectedFactoryId = R.id.news_feed;
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
    protected int getLeftNavigationDrawerMenuResource() {
        return R.menu.home_nav_drawer;
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
    public boolean onNavigationItemSelected(MenuItem item) {
        super.onNavigationItemSelected(item);

        int id = item.getItemId();
        FragmentFactory factory = getFactoryForItem(id, null);

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

    private FragmentFactory getFactoryForItem(int id, Bundle savedInstanceState) {
        switch (id) {
            case R.id.news_feed:
                return new NewsFeedFactory(this, mUserLogin);
            case R.id.my_repos:
                return new RepositoryFactory(this, mUserLogin, savedInstanceState);
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
    protected Fragment getFragment(int position) {
        Fragment fragment = mFactory.getFragment(position);
        mFragments.put(position, fragment);
        return fragment;
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
