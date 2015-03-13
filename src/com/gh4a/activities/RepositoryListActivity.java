package com.gh4a.activities;

import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.internal.view.SupportMenuInflater;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListAdapter;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.fragment.RepositoryListFragment;
import com.gh4a.fragment.RepositorySearchFragment;
import com.gh4a.fragment.StarredRepositoryListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;
import com.gh4a.utils.UiUtils;

import java.util.ArrayList;
import java.util.List;

public class RepositoryListActivity extends BaseActivity implements
        SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener {
    private String mUserLogin;
    private String mUserType;

    private MenuInflater mMenuInflater;
    private RepositorySearchFragment mSearchFragment;
    private List<DrawerAdapter.Item> mDrawerItems;
    private DrawerAdapter mDrawerAdapter;
    private String[] mTypes;
    private int mSelectedIndex = 0;
    private MenuItem mFilterItem;

    private static final String STATE_KEY_INDEX = "selectedIndex";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserType = data.getString(Constants.User.TYPE);

        if (savedInstanceState != null) {
            mSelectedIndex = savedInstanceState.getInt(STATE_KEY_INDEX);
        }

        super.onCreate(savedInstanceState);

        if (hasErrorView()) {
            return;
        }

        mMenuInflater = new SupportMenuInflater(UiUtils.makeHeaderThemedContext(this));
        setContentView(R.layout.frame_layout);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.user_pub_repos);
        actionBar.setSubtitle(mUserLogin);
        actionBar.setDisplayHomeAsUpEnabled(true);

        addSearchFragment();
    }

    @Override
    protected ListAdapter getNavigationDrawerAdapter() {
        final int itemResId, valueResId;

        if (mUserLogin.equals(Gh4Application.get().getAuthLogin())) {
            itemResId = R.array.repo_list_login_items;
            valueResId = R.array.repo_list_login_values;
        } else if (Constants.User.TYPE_ORG.equals(mUserType)) {
            itemResId = R.array.repo_list_org_items;
            valueResId = R.array.repo_list_org_values;
        } else {
            itemResId = R.array.repo_list_user_items;
            valueResId = R.array.repo_list_user_values;
        }

        mTypes = getResources().getStringArray(valueResId);
        mDrawerItems = new ArrayList<>();

        TypedArray itemResIds = getResources().obtainTypedArray(itemResId);
        for (int i = 0; i < itemResIds.length(); i++) {
            mDrawerItems.add(new DrawerAdapter.RadioItem(itemResIds.getResourceId(i, 0), i + 1));
        }

        itemResIds.recycle();

        mDrawerAdapter = new DrawerAdapter(this, mDrawerItems);
        onDrawerItemSelected(mSelectedIndex);

        return mDrawerAdapter;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_KEY_INDEX, mSelectedIndex);
    }

    @Override
    protected boolean isRightSideDrawer() {
        return true;
    }

    @Override
    protected boolean onDrawerItemSelected(int position) {
        Fragment fragment = null;

        if (position < mTypes.length) {
            String type = mTypes[position];
            switch (type) {
                case "starred":
                    fragment = StarredRepositoryListFragment.newInstance(mUserLogin);
                    break;
                case "watched":
                    fragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                    break;
                default:
                    fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, type);
                    break;
            }
        }

        if (fragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment old = fm.findFragmentByTag("main");

            if (old != null) {
                ft.remove(old);
            }
            ft.add(R.id.details, fragment, "main");
            ft.commitAllowingStateLoss();
        }

        mSelectedIndex = position;
        for (int i = 0; i < mDrawerItems.size(); i++) {
            DrawerAdapter.RadioItem item = (DrawerAdapter.RadioItem) mDrawerItems.get(i);
            item.setChecked(position == i);
        }
        mDrawerAdapter.notifyDataSetChanged();

        super.supportInvalidateOptionsMenu();
        return true;
    }

    private void addSearchFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        mSearchFragment = RepositorySearchFragment.newInstance(mUserLogin);
        mSearchFragment.setUserVisibleHint(false);
        ft.add(R.id.details, mSearchFragment, "search");
        ft.hide(mSearchFragment);
        ft.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mMenuInflater.inflate(R.menu.repo_list_menu, menu);

        // We can only properly search the 'all repos' list
        if (mSelectedIndex == 0) {
            MenuItem searchItem = menu.findItem(R.id.search);
            MenuItemCompat.setOnActionExpandListener(searchItem, this);

            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        } else {
            menu.removeItem(R.id.search);
        }

        mFilterItem = menu.findItem(R.id.filter);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                setSearchVisibility(true);
                return true;
            case R.id.filter:
                toggleDrawer();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setSearchVisibility(boolean visible) {
        String hiddenTag = visible ? "main" : "search";
        String visibleTag = visible ? "search" : "main";
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        mFilterItem.setVisible(!visible);

        mSearchFragment.setQuery(null);
        mSearchFragment.setUserVisibleHint(visible);
        ft.hide(fm.findFragmentByTag(hiddenTag));
        ft.show(fm.findFragmentByTag(visibleTag));
        ft.commit();
    }

    @Override
    public void supportInvalidateOptionsMenu() {
        // happens when load is done; we ignore it as we don't want to close the IME in that case
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (mSearchFragment != null) {
            mSearchFragment.setQuery(query);
        }
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public boolean onClose() {
        setSearchVisibility(false);
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        setSearchVisibility(false);
        return true;
    }
}