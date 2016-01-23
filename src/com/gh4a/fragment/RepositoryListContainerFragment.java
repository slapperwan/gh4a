package com.gh4a.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.DrawerAdapter;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.SwipeRefreshLayout;

import org.eclipse.egit.github.core.Repository;

import java.util.ArrayList;
import java.util.List;

public class RepositoryListContainerFragment extends Fragment implements
        SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItemCompat.OnActionExpandListener, SwipeRefreshLayout.ChildScrollDelegate {
    private String mUserLogin;
    private String mUserType;
    private String mFilterType;

    private MenuInflater mMenuInflater;
    private PagedDataBaseFragment<Repository> mMainFragment;
    private RepositorySearchFragment mSearchFragment;
    private MenuItem mFilterItem;

    public interface Callback {
        void initiateFilter();
    }

    private static final String STATE_KEY_FILTER_TYPE = "filter_type";

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
        }

        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
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

            SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
            searchView.setOnCloseListener(this);
            searchView.setOnQueryTextListener(this);
        } else {
            menu.removeItem(R.id.search);
        }

        mFilterItem = menu.findItem(R.id.filter);

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

        mFilterItem.setVisible(!visible);

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

    public static class FilterDrawerAdapter extends DrawerAdapter {
        private String[] mTypes;
        private List<Item> mItems;
        private int mSelectedIndex = 0;

        public static FilterDrawerAdapter create(Context context, String userLogin, String userType) {
            final int itemResId, valueResId;

            if (userLogin.equals(Gh4Application.get().getAuthLogin())) {
                itemResId = R.array.repo_list_login_items;
                valueResId = R.array.repo_list_login_values;
            } else if (Constants.User.TYPE_ORG.equals(userType)) {
                itemResId = R.array.repo_list_org_items;
                valueResId = R.array.repo_list_org_values;
            } else {
                itemResId = R.array.repo_list_user_items;
                valueResId = R.array.repo_list_user_values;
            }

            String[] types = context.getResources().getStringArray(valueResId);
            List<Item> items = new ArrayList<>();

            TypedArray itemResIds = context.getResources().obtainTypedArray(itemResId);
            for (int i = 0; i < itemResIds.length(); i++) {
                items.add(new DrawerAdapter.RadioItem(itemResIds.getResourceId(i, 0), i + 1));
            }

            itemResIds.recycle();

            return new FilterDrawerAdapter(context, items, types);
        }

        private FilterDrawerAdapter(Context context, List<Item> items, String[] types) {
            super(context, items);
            mTypes = types;
            mItems = items;
        }

        public String handleSelectionAndGetFilterType(int position) {
            if (position >= mTypes.length) {
                return null;
            }

            for (int i = 0; i < mItems.size(); i++) {
                DrawerAdapter.RadioItem item = (DrawerAdapter.RadioItem) mItems.get(i);
                item.setChecked(position == i);
            }

            return mTypes[position];
        }
    }
}