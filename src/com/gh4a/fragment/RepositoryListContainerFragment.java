package com.gh4a.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.widget.SearchView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import org.eclipse.egit.github.core.Repository;

public class RepositoryListContainerFragment extends Fragment implements
        SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener, SwipeRefreshLayout.ChildScrollDelegate {
    private String mUserLogin;
    private String mUserType;
    private String mFilterType;
    private boolean mSearchVisible;

    private MenuInflater mMenuInflater;
    private PagedDataBaseFragment<Repository> mMainFragment;
    private RepositorySearchFragment mSearchFragment;
    private MenuItem mFilterItem;
    private String mSearchQuery;

    public interface Callback {
        void initiateFilter();
    }

    private static final String STATE_KEY_FILTER_TYPE = "filter_type";
    private static final String STATE_KEY_SEARCH_VISIBLE = "search_visible";
    private static final String STATE_KEY_QUERY = "search_query";

    public static RepositoryListContainerFragment newInstance(String userLogin, String userType) {
        RepositoryListContainerFragment f = new RepositoryListContainerFragment();
        Bundle args = new Bundle();

        args.putString(Constants.User.LOGIN, userLogin);
        args.putString(Constants.User.TYPE, userType);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getArguments();
        mUserLogin = data.getString(Constants.User.LOGIN);
        mUserType = data.getString(Constants.User.TYPE);

        if (savedInstanceState != null) {
            mFilterType = savedInstanceState.getString(STATE_KEY_FILTER_TYPE);
            mSearchVisible = savedInstanceState.getBoolean(STATE_KEY_SEARCH_VISIBLE);
            mSearchQuery = savedInstanceState.getString(STATE_KEY_QUERY);
        }

        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    public void destroyChildren() {
        getChildFragmentManager().beginTransaction()
                .remove(mMainFragment)
                .remove(mSearchFragment)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frame_layout, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mMenuInflater = new SupportMenuInflater(UiUtils.makeHeaderThemedContext(getActivity()));

        FragmentManager fm = getChildFragmentManager();
        mMainFragment = (PagedDataBaseFragment<Repository>) fm.findFragmentByTag("main");
        mSearchFragment = (RepositorySearchFragment) fm.findFragmentByTag("search");

        if (mMainFragment == null) {
            setFilterType("all");
            addSearchFragment();
        } else {
            setSearchVisibility(mSearchVisible);
        }
    }

    @Override
    public boolean canChildScrollUp() {
        if (mFilterItem != null && !mFilterItem.isVisible()) {
            // search mode
            return mSearchFragment.canChildScrollUp();
        }
        return mMainFragment.canChildScrollUp();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_KEY_FILTER_TYPE, mFilterType);
        outState.putBoolean(STATE_KEY_SEARCH_VISIBLE, mSearchVisible);
        outState.putString(STATE_KEY_QUERY, mSearchQuery);
    }

    public void refresh() {
        mSearchFragment.refresh();
        mMainFragment.refresh();
    }

    public void setFilterType(String type) {
        switch (type) {
            case "starred":
                mMainFragment = StarredRepositoryListFragment.newInstance(mUserLogin);
                break;
            case "watched":
                mMainFragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                break;
            default:
                mMainFragment = RepositoryListFragment.newInstance(mUserLogin, mUserType, type);
                break;
        }

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment old = fm.findFragmentByTag("main");

        if (old != null) {
            ft.remove(old);
        }
        ft.add(R.id.details, mMainFragment, "main");
        ft.commitAllowingStateLoss();
        mFilterType = type;
    }

    private void addSearchFragment() {
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();

        mSearchFragment = RepositorySearchFragment.newInstance(mUserLogin);
        mSearchFragment.setUserVisibleHint(false);
        ft.add(R.id.details, mSearchFragment, "search");
        ft.hide(mSearchFragment);
        ft.commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuInflater.inflate(R.menu.repo_list_menu, menu);

        // We can only properly search the 'all repos' list
        if ("all".equals(mFilterType)) {
            MenuItem searchItem = menu.findItem(R.id.search);
            MenuItemCompat.setOnActionExpandListener(searchItem, this);

            final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            if (mSearchQuery != null) {
                MenuItemCompat.expandActionView(searchItem);
                searchView.setQuery(mSearchQuery, false);
            }
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        } else {
            menu.removeItem(R.id.search);
        }

        mFilterItem = menu.findItem(R.id.filter);
        mFilterItem.setVisible(!mSearchVisible);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                setSearchVisibility(true);
                return true;
            case R.id.filter:
                if (getActivity() instanceof Callback) {
                    ((Callback) getActivity()).initiateFilter();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setSearchVisibility(boolean visible) {
        String hiddenTag = visible ? "main" : "search";
        String visibleTag = visible ? "search" : "main";
        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        mSearchVisible = visible;
        if (mFilterItem != null) {
            mFilterItem.setVisible(!visible);
        }

        mSearchFragment.setQuery(null);
        mSearchFragment.setUserVisibleHint(visible);
        ft.hide(fm.findFragmentByTag(hiddenTag));
        ft.show(fm.findFragmentByTag(visibleTag));
        ft.commit();
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
        mSearchQuery = newText;
        return false;
    }

    @Override
    public boolean onClose() {
        mSearchQuery = null;
        setSearchVisibility(false);
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mSearchQuery = null;
        setSearchVisibility(false);
        return true;
    }

    public static class FilterDrawerHelper {
        private int mMenuResId;

        private static SparseArray<String> FILTER_LOOKUP = new SparseArray<>();
        static {
            FILTER_LOOKUP.put(R.id.filter_type_all, "all");
            FILTER_LOOKUP.put(R.id.filter_type_owner, "owner");
            FILTER_LOOKUP.put(R.id.filter_type_member, "member");
            FILTER_LOOKUP.put(R.id.filter_type_public, "public");
            FILTER_LOOKUP.put(R.id.filter_type_private, "private");
            FILTER_LOOKUP.put(R.id.filter_type_sources, "sources");
            FILTER_LOOKUP.put(R.id.filter_type_forks, "forks");
            FILTER_LOOKUP.put(R.id.filter_type_watched, "watched");
            FILTER_LOOKUP.put(R.id.filter_type_starred, "starred");
        }

        public static FilterDrawerHelper create(Context context, String userLogin, String userType) {
            int menuResId;
            if (userLogin.equals(Gh4Application.get().getAuthLogin())) {
                menuResId = R.menu.repo_filter_logged_in;
            } else if (Constants.User.TYPE_ORG.equals(userType)) {
                menuResId = R.menu.repo_filter_org;
            } else {
                menuResId = R.menu.repo_filter_user;
            }

            return new FilterDrawerHelper(menuResId);
        }

        private FilterDrawerHelper(int menuResId) {
            mMenuResId = menuResId;
        }

        public int[] getMenuResIds() {
            return new int[] { mMenuResId };
        }

        public String handleSelectionAndGetFilterType(MenuItem item) {
            return FILTER_LOOKUP.get(item.getItemId());
        }
    }
}