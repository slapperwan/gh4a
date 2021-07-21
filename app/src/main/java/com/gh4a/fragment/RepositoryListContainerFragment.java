package com.gh4a.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.Lifecycle;

import android.text.TextUtils;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.widget.SwipeRefreshLayout;
import com.meisolsson.githubsdk.model.Repository;

public class RepositoryListContainerFragment extends Fragment implements
        BaseActivity.RefreshableChild, SearchView.OnCloseListener, SearchView.OnQueryTextListener,
        MenuItem.OnActionExpandListener, SwipeRefreshLayout.ChildScrollDelegate {
    public static RepositoryListContainerFragment newInstance(String userLogin, boolean isOrg) {
        return newInstance(userLogin, isOrg, null);
    }

    public static RepositoryListContainerFragment newInstance(String userLogin, boolean isOrg,
            String defaultFilter) {
        RepositoryListContainerFragment f = new RepositoryListContainerFragment();
        Bundle args = new Bundle();

        args.putString("user", userLogin);
        args.putBoolean("is_org", isOrg);
        args.putString("filter_type", defaultFilter);
        f.setArguments(args);

        return f;
    }

    public static final String FILTER_TYPE_STARRED = "starred";

    private String mUserLogin;
    private boolean mIsOrg;
    private String mFilterType;
    private String mSortOrder = "full_name";
    private String mSortDirection = "asc";
    private boolean mSearchVisible;

    private PagedDataBaseFragment<Repository> mMainFragment;
    private RepositorySearchFragment mSearchFragment;
    private MenuItem mFilterItem;
    private String mSearchQuery;
    private boolean mSearchIsExpanded;

    public interface Callback {
        void initiateFilter();
    }

    private static final String STATE_KEY_FILTER_TYPE = "filter_type";
    private static final String STATE_KEY_SORT_ORDER = "sort_order";
    private static final String STATE_KEY_SORT_DIRECTION = "sort_direction";
    private static final String STATE_KEY_SEARCH_VISIBLE = "search_visible";
    private static final String STATE_KEY_QUERY = "search_query";
    private static final String STATE_KEY_SEARCH_IS_EXPANDED = "search_is_expanded";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getArguments();
        mUserLogin = data.getString("user");
        mIsOrg = data.getBoolean("is_org");

        // Only read filter type from arguments if it wasn't overridden already by our parent
        if (mFilterType == null) {
            mFilterType = data.getString("filter_type", "all");
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_KEY_FILTER_TYPE)) {
            mFilterType = savedInstanceState.getString(STATE_KEY_FILTER_TYPE);
            mSortOrder = savedInstanceState.getString(STATE_KEY_SORT_ORDER);
            mSortDirection = savedInstanceState.getString(STATE_KEY_SORT_DIRECTION);
            mSearchVisible = savedInstanceState.getBoolean(STATE_KEY_SEARCH_VISIBLE);
            mSearchQuery = savedInstanceState.getString(STATE_KEY_QUERY);
            mSearchIsExpanded = savedInstanceState.getBoolean(STATE_KEY_SEARCH_IS_EXPANDED);
        }

        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onRefresh() {
        mMainFragment.onRefresh();
        mSearchFragment.onRefresh();
    }

    public void destroyChildren() {
        getChildFragmentManager().beginTransaction()
                .remove(mMainFragment)
                .remove(mSearchFragment)
                .commitNowAllowingStateLoss();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.frame_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        mMainFragment = (PagedDataBaseFragment<Repository>) fm.findFragmentByTag("main");
        mSearchFragment = (RepositorySearchFragment) fm.findFragmentByTag("search");

        if (mMainFragment == null) {
            applyFilterTypeAndSortOrder();
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
        outState.putString(STATE_KEY_SORT_ORDER, mSortOrder);
        outState.putString(STATE_KEY_SORT_DIRECTION, mSortDirection);
        outState.putBoolean(STATE_KEY_SEARCH_VISIBLE, mSearchVisible);
        outState.putString(STATE_KEY_QUERY, mSearchQuery);
        outState.putBoolean(STATE_KEY_SEARCH_IS_EXPANDED, mSearchIsExpanded);
    }

    public void setFilterType(String type) {
        if (!TextUtils.equals(type, mFilterType)) {
            mFilterType = type;
            applyFilterTypeAndSortOrder();
        }
    }

    public void setSortOrder(String sortOrder, String direction) {
        if (!TextUtils.equals(sortOrder, mSortOrder) || !TextUtils.equals(mSortDirection, direction)) {
            mSortOrder = sortOrder;
            mSortDirection = direction;
            validateSortOrder();
            applyFilterTypeAndSortOrder();
        }
    }

    private void validateSortOrder() {
        if (TextUtils.equals(mFilterType, "starred")) {
            if (!TextUtils.equals(mSortOrder, "updated")
                    && !TextUtils.equals(mSortOrder, "created")) {
                mSortOrder = "created";
                mSortDirection = "desc";
            }
        } else {
            if (!TextUtils.equals(mSortOrder, "full_name")
                    && !TextUtils.equals(mSortOrder, "created")
                    && !TextUtils.equals(mSortOrder, "pushed")) {
                mSortOrder = "full_name";
                mSortDirection = "asc";
            }
        }
    }

    private void applyFilterTypeAndSortOrder() {
        if (!isAdded()) {
            // we'll do this in onActivityCreated()
            return;
        }

        switch (mFilterType) {
            case "watched":
                mMainFragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                break;
            default:
                mMainFragment = RepositoryListFragment.newInstance(mUserLogin, mIsOrg,
                        mFilterType, mSortOrder, mSortDirection);
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
    }

    public String getFilterType() {
        return mFilterType;
    }

    public String getSortOrder() {
        validateSortOrder();
        return mSortOrder;
    }

    public String getSortDirection() {
        return mSortDirection;
    }

    private void addSearchFragment() {
        mSearchFragment = RepositorySearchFragment.newInstance(mUserLogin);

        getChildFragmentManager().beginTransaction()
                .add(R.id.details, mSearchFragment, "search")
                .hide(mSearchFragment)
                .setMaxLifecycle(mSearchFragment, Lifecycle.State.STARTED)
                .commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.repo_list_menu, menu);

        // We can only properly search the 'all repos' list
        if ("all".equals(mFilterType)) {
            MenuItem searchItem = menu.findItem(R.id.search);
            searchItem.setOnActionExpandListener(this);

            final SearchView searchView = (SearchView) searchItem.getActionView();
            if (mSearchIsExpanded) {
                searchItem.expandActionView();
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

        mSearchVisible = visible;
        if (mFilterItem != null) {
            mFilterItem.setVisible(!visible);
        }

        mSearchFragment.setQuery(null);

        final FragmentManager fm = getChildFragmentManager();
        fm.beginTransaction()
                .setMaxLifecycle(mSearchFragment, visible ? Lifecycle.State.RESUMED : Lifecycle.State.STARTED)
                .hide(fm.findFragmentByTag(hiddenTag))
                .show(fm.findFragmentByTag(visibleTag))
                .commit();
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
        mSearchIsExpanded = false;
        mSearchQuery = null;
        setSearchVisibility(false);
        return false;
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        mSearchIsExpanded = true;
        return true;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        mSearchIsExpanded = false;
        mSearchQuery = null;
        setSearchVisibility(false);
        return true;
    }

    private static void setMenuItemChecked(MenuItem item, int selected, SparseArray<?> relevant) {
        if (relevant.indexOfKey(item.getItemId()) >= 0) {
            item.setChecked(item.getItemId() == selected);
        }
    }

    private static void setMenuItemChecked(Menu menu, int selected, SparseArray<?> relevant) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.hasSubMenu()) {
                SubMenu sm = item.getSubMenu();
                for (int j = 0; j < sm.size(); j++) {
                    setMenuItemChecked(sm.getItem(j), selected, relevant);
                }
            } else {
                setMenuItemChecked(item, selected, relevant);
            }
        }
    }

    public static class FilterDrawerHelper {
        private final int mMenuResId;

        private static final SparseArray<String> FILTER_LOOKUP = new SparseArray<>();
        static {
            FILTER_LOOKUP.put(R.id.filter_type_all, "all");
            FILTER_LOOKUP.put(R.id.filter_type_owner, "owner");
            FILTER_LOOKUP.put(R.id.filter_type_member, "member");
            FILTER_LOOKUP.put(R.id.filter_type_public, "public");
            FILTER_LOOKUP.put(R.id.filter_type_private, "private");
            FILTER_LOOKUP.put(R.id.filter_type_sources, "sources");
            FILTER_LOOKUP.put(R.id.filter_type_forks, "forks");
            FILTER_LOOKUP.put(R.id.filter_type_watched, "watched");
        }

        public static FilterDrawerHelper create(String userLogin, boolean isOrg) {
            int menuResId;
            if (ApiHelpers.loginEquals(userLogin, Gh4Application.get().getAuthLogin())) {
                menuResId = R.menu.repo_filter_logged_in;
            } else if (isOrg) {
                menuResId = R.menu.repo_filter_org;
            } else {
                menuResId = R.menu.repo_filter_user;
            }

            return new FilterDrawerHelper(menuResId);
        }

        private FilterDrawerHelper(int menuResId) {
            mMenuResId = menuResId;
        }

        public int getMenuResId() {
            return mMenuResId;
        }

        public void selectFilterType(Menu menu, String filterType) {
            int selectedId = 0;
            for (int i = 0; i < FILTER_LOOKUP.size(); i++) {
                if (FILTER_LOOKUP.valueAt(i).equals(filterType)) {
                    selectedId = FILTER_LOOKUP.keyAt(i);
                    break;
                }
            }
            setMenuItemChecked(menu, selectedId, FILTER_LOOKUP);
        }

        public String handleSelectionAndGetFilterType(MenuItem item) {
            return FILTER_LOOKUP.get(item.getItemId());
        }
    }

    public static class SortDrawerHelper {
        private String mFilterType = "all";

        private static final SparseArray<String[]> SORT_LOOKUP = new SparseArray<>();
        static {
            SORT_LOOKUP.put(R.id.sort_name_asc, new String[] { "full_name", "asc" });
            SORT_LOOKUP.put(R.id.sort_name_desc, new String[] { "full_name", "desc" });
            SORT_LOOKUP.put(R.id.sort_created_asc, new String[] { "created", "asc" });
            SORT_LOOKUP.put(R.id.sort_created_desc, new String[] { "created", "desc" });
            SORT_LOOKUP.put(R.id.sort_pushed_asc, new String[] { "pushed", "asc" });
            SORT_LOOKUP.put(R.id.sort_pushed_desc, new String[] { "pushed", "desc" });
            SORT_LOOKUP.put(R.id.sort_updated_asc, new String[] { "updated", "asc" });
            SORT_LOOKUP.put(R.id.sort_updated_desc, new String[] { "updated", "desc" });
        }

        public SortDrawerHelper() {
        }

        public void setFilterType(String type) {
            mFilterType = type;
        }

        public int getMenuResId() {
            return TextUtils.equals(mFilterType, "watched") ? 0 : R.menu.repo_sort;
        }

        public void selectSortType(Menu menu, String order, String direction,
                boolean updateSingleItem) {
            int selectedId = 0;
            for (int i = 0; i < SORT_LOOKUP.size(); i++) {
                String[] value = SORT_LOOKUP.valueAt(i);
                if (value[0].equals(order) && value[1].equals(direction)) {
                    selectedId = SORT_LOOKUP.keyAt(i);
                    if (updateSingleItem) {
                        menu.findItem(selectedId).setChecked(true);
                        return;
                    }
                    break;
                }
            }
            setMenuItemChecked(menu, selectedId, SORT_LOOKUP);
        }

        public String[] handleSelectionAndGetSortOrder(MenuItem item) {
            return SORT_LOOKUP.get(item.getItemId());
        }
    }
}