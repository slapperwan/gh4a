package com.gh4a.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.RepositoryListFragment;
import com.gh4a.fragment.RepositorySearchFragment;
import com.gh4a.fragment.StarredRepositoryListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;

public class RepositoryListActivity extends BaseFragmentActivity implements
        ActionBar.OnNavigationListener, SearchView.OnCloseListener,
        SearchView.OnQueryTextListener, MenuItemCompat.OnActionExpandListener {
    private String mUserLogin;
    private String mUserType;

    private ActionBar mActionBar;
    private RepositorySearchFragment mSearchFragment;
    private String[] mTypes;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle data = getIntent().getExtras();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserType = data.getString(Constants.User.TYPE);

        if (!isOnline()) {
            setErrorView();
            return;
        }

        setContentView(R.layout.frame_layout);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mUserLogin);
        mActionBar.setSubtitle(R.string.user_pub_repos);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        Context context = mActionBar.getThemedContext();
        final int items, values;

        if (mUserLogin.equals(Gh4Application.get(this).getAuthLogin())) {
            items = R.array.repo_list_login_items;
            values = R.array.repo_list_login_values;
        } else if (Constants.User.TYPE_ORG.equals(mUserType)) {
            items = R.array.repo_list_org_items;
            values = R.array.repo_list_org_values;
        } else {
            items = R.array.repo_list_user_items;
            values = R.array.repo_list_user_values;
        }

        mTypes = getResources().getStringArray(values);

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                android.R.layout.simple_spinner_item, getResources().getStringArray(items));
        adapter.setDropDownViewResource(R.layout.row_simple);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(adapter, this);
        addSearchFragment();
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
        getMenuInflater().inflate(R.menu.repo_list_menu, menu);

        // We can only properly search the 'all repos' list
        if (mActionBar.getSelectedNavigationIndex() == 0) {
            MenuItem searchItem = menu.findItem(R.id.search);
            MenuItemCompat.setOnActionExpandListener(searchItem, this);

            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        } else {
            menu.removeItem(R.id.search);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            setSearchVisibility(true);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setSearchVisibility(boolean visible) {
        String hiddenTag = visible ? "main" : "search";
        String visibleTag = visible ? "search" : "main";
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        mSearchFragment.setQuery(null);
        mSearchFragment.setUserVisibleHint(visible);
        ft.hide(fm.findFragmentByTag(hiddenTag));
        ft.show(fm.findFragmentByTag(visibleTag));
        ft.commit();

        mActionBar.setNavigationMode(visible
                ? ActionBar.NAVIGATION_MODE_STANDARD : ActionBar.NAVIGATION_MODE_LIST);
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

    @Override
    public boolean onNavigationItemSelected(int position, long itemId) {
        Fragment fragment = null;

        if (position < mTypes.length) {
            String type = mTypes[position];
            if (type.equals("starred")) {
                fragment = StarredRepositoryListFragment.newInstance(mUserLogin);
            } else if (type.equals("watched")) {
                fragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
            } else {
                fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, type);
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
        super.supportInvalidateOptionsMenu();
        return true;
    }
}