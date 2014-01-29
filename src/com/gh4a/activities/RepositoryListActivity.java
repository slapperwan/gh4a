package com.gh4a.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.SearchView;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.RepositoryListFragment;
import com.gh4a.fragment.RepositorySearchFragment;
import com.gh4a.fragment.StarredRepositoryListFragment;
import com.gh4a.fragment.WatchedRepositoryListFragment;

public class RepositoryListActivity extends BaseSherlockFragmentActivity implements
        ActionBar.OnNavigationListener, SearchView.OnCloseListener,
        SearchView.OnQueryTextListener, MenuItem.OnActionExpandListener {
    public String mUserLogin;
    public String mUserType;
    
    private ActionBar mActionBar;
    private RepositorySearchFragment mSearchFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
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
        final int items;
        if (Constants.User.TYPE_ORG.equals(mUserType)) {
            items = R.array.repo_org_item;
        } else if (mUserLogin.equals(Gh4Application.get(this).getAuthLogin())) {
            items = R.array.repo_login_item;
        } else {
            items = R.array.repo_user_item;
        }

        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(context,
                R.layout.sherlock_spinner_item, getResources().getStringArray(items));
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
        getSupportMenuInflater().inflate(R.menu.repo_list_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search);
        searchItem.setOnActionExpandListener(this);

        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnCloseListener(this);
        searchView.setOnQueryTextListener(this);

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
    public void invalidateOptionsMenu() {
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
        boolean isSelf = mUserLogin.equals(Gh4Application.get(this).getAuthLogin());
        ListFragment fragment = null;

        if (position == 0) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "all");
        } else if (position == 1) {
            fragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
        } else if (position == 2) {
            fragment = StarredRepositoryListFragment.newInstance(mUserLogin);
        } else if (position == 3 && isSelf) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "public");
        } else if (position == 4 && isSelf) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "private");
        } else if ((position == 5 && isSelf) || (position == 3 && !isSelf)) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "sources");
        } else if ((position == 6 && isSelf) || (position == 4 && !isSelf)) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "forks");
        } else if ((position == 7 && isSelf) || (position == 5 && !isSelf)) {
            fragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, "member");
        }
        
        if (fragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            Fragment old = fm.findFragmentByTag("main");

            if (old != null) {
                ft.remove(old);
            }
            ft.add(R.id.details, fragment, "main");
            ft.commit();
        }
        return true;
    }
    
    @Override
    protected void navigateUp() {
        finish();
    }
}