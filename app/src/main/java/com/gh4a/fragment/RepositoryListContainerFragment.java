package com.gh4a.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SearchView;
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
    private static final String EXTRA_USER = "user";
    private static final String EXTRA_IS_ORG = "is_org";
    private static final String EXTRA_FILTER_TYPE = "filter_type";
    private static final String STATE_KEY_FILTER_TYPE = "filter_type";
    private static final String STATE_KEY_SORT_ORDER = "sort_order";
    private static final String STATE_KEY_SORT_DIRECTION = "sort_direction";
    private static final String STATE_KEY_SEARCH_VISIBLE = "search_visible";
    private static final String STATE_KEY_QUERY = "search_query";
    private static final String STATE_KEY_SEARCH_IS_EXPANDED = "search_is_expanded";
    private static final String SORT_DIRECTION_ASC = "asc";
    private static final String SORT_DIRECTION_DESC = "desc";
    private static final String SORT_ORDER_FULL_NAME = "full_name";
    private static final String SORT_ORDER_UPDATED = "updated";
    private static final String SORT_ORDER_CREATED = "created";
    private static final String SORT_ORDER_PUSHED = "pushed";
    private static final String TAG_MAIN = "main";
    private static final String TAG_SEARCH = "search";
    private static final String FILTER_TYPE_ALL = "all";
    private static final String FILTER_TYPE_OWNER = "owner";
    private static final String FILTER_TYPE_MEMBER = "member";
    private static final String FILTER_TYPE_PUBLIC = "public";
    private static final String FILTER_TYPE_PRIVATE = "private";
    private static final String FILTER_TYPE_SOURCES = "sources";
    private static final String FILTER_TYPE_FORKS = "forks";
    private static final String FILTER_TYPE_WATCHED = "watched";
    public static final String FILTER_TYPE_STARRED = "starred";

    public static RepositoryListContainerFragment newInstance(String userLogin, boolean isOrg) {
        return newInstance(userLogin, isOrg, null);
    }

    public static RepositoryListContainerFragment newInstance(String userLogin, boolean isOrg,
            String defaultFilter) {
        RepositoryListContainerFragment f = new RepositoryListContainerFragment();
        Bundle args = new Bundle();

        args.putString(EXTRA_USER, userLogin);
        args.putBoolean(EXTRA_IS_ORG, isOrg);
        args.putString(EXTRA_FILTER_TYPE, defaultFilter);
        f.setArguments(args);

        return f;
    }

    private String mUserLogin;
    private boolean mIsOrg;
    private String mFilterType;
    private String mSortOrder = SORT_ORDER_FULL_NAME;
    private String mSortDirection = SORT_DIRECTION_ASC;
    private boolean mSearchVisible;

    private PagedDataBaseFragment<Repository> mMainFragment;
    private RepositorySearchFragment mSearchFragment;
    private MenuItem mFilterItem;
    private String mSearchQuery;
    private boolean mSearchIsExpanded;

    public interface Callback {
        void initiateFilter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Bundle data = getArguments();
        mUserLogin = data.getString(EXTRA_USER);
        mIsOrg = data.getBoolean(EXTRA_IS_ORG);

        // Only read filter type from arguments if it wasn't overridden already by our parent
        if (mFilterType == null) {
            mFilterType = data.getString(EXTRA_FILTER_TYPE, FILTER_TYPE_ALL);
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
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fm = getChildFragmentManager();
        mMainFragment = (PagedDataBaseFragment<Repository>) fm.findFragmentByTag(TAG_MAIN);
        mSearchFragment = (RepositorySearchFragment) fm.findFragmentByTag(TAG_SEARCH);

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
        if (TextUtils.equals(mFilterType, FILTER_TYPE_STARRED)) {
            if (!TextUtils.equals(mSortOrder, SORT_ORDER_UPDATED)
                    && !TextUtils.equals(mSortOrder, SORT_ORDER_CREATED)) {
                mSortOrder = SORT_ORDER_CREATED;
                mSortDirection = SORT_DIRECTION_DESC;
            }
        } else {
            if (!TextUtils.equals(mSortOrder, SORT_ORDER_FULL_NAME)
                    && !TextUtils.equals(mSortOrder, SORT_ORDER_CREATED)
                    && !TextUtils.equals(mSortOrder, SORT_ORDER_PUSHED)) {
                mSortOrder = SORT_ORDER_FULL_NAME;
                mSortDirection = SORT_DIRECTION_ASC;
            }
        }
    }

    private void applyFilterTypeAndSortOrder() {
        if (!isAdded()) {
            // we'll do this in onActivityCreated()
            return;
        }

        switch (mFilterType) {
            case FILTER_TYPE_WATCHED:
                mMainFragment = WatchedRepositoryListFragment.newInstance(mUserLogin);
                break;
            default:
                mMainFragment = RepositoryListFragment.newInstance(mUserLogin, mIsOrg,
                        mFilterType, mSortOrder, mSortDirection);
                break;
        }

        FragmentManager fm = getChildFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment old = fm.findFragmentByTag(TAG_MAIN);

        if (old != null) {
            ft.remove(old);
        }
        ft.add(R.id.details, mMainFragment, TAG_MAIN);
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
        FragmentTransaction ft = getChildFragmentManager().beginTransaction();

        mSearchFragment = RepositorySearchFragment.newInstance(mUserLogin);
        mSearchFragment.setUserVisibleHint(false);
        ft.add(R.id.details, mSearchFragment, TAG_SEARCH);
        ft.hide(mSearchFragment);
        ft.commit();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        getActivity().getMenuInflater().inflate(R.menu.repo_list_menu, menu);

        // We can only properly search the 'all repos' list
        if (FILTER_TYPE_ALL.equals(mFilterType)) {
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
        String hiddenTag = visible ? TAG_MAIN : TAG_SEARCH;
        String visibleTag = visible ? TAG_SEARCH : TAG_MAIN;
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
            FILTER_LOOKUP.put(R.id.filter_type_all, FILTER_TYPE_ALL);
            FILTER_LOOKUP.put(R.id.filter_type_owner, FILTER_TYPE_OWNER);
            FILTER_LOOKUP.put(R.id.filter_type_member, FILTER_TYPE_MEMBER);
            FILTER_LOOKUP.put(R.id.filter_type_public, FILTER_TYPE_PUBLIC);
            FILTER_LOOKUP.put(R.id.filter_type_private, FILTER_TYPE_PRIVATE);
            FILTER_LOOKUP.put(R.id.filter_type_sources, FILTER_TYPE_SOURCES);
            FILTER_LOOKUP.put(R.id.filter_type_forks, FILTER_TYPE_FORKS);
            FILTER_LOOKUP.put(R.id.filter_type_watched, FILTER_TYPE_WATCHED);
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
        private String mFilterType = FILTER_TYPE_ALL;

        private static final SparseArray<String[]> SORT_LOOKUP = new SparseArray<>();
        static {
            SORT_LOOKUP.put(R.id.sort_name_asc, new String[] { SORT_ORDER_FULL_NAME, SORT_DIRECTION_ASC });
            SORT_LOOKUP.put(R.id.sort_name_desc, new String[] { SORT_ORDER_FULL_NAME, SORT_DIRECTION_DESC });
            SORT_LOOKUP.put(R.id.sort_created_asc, new String[] { SORT_ORDER_CREATED, SORT_DIRECTION_ASC });
            SORT_LOOKUP.put(R.id.sort_created_desc, new String[] { SORT_ORDER_CREATED, SORT_DIRECTION_DESC });
            SORT_LOOKUP.put(R.id.sort_pushed_asc, new String[] { SORT_ORDER_PUSHED, SORT_DIRECTION_ASC });
            SORT_LOOKUP.put(R.id.sort_pushed_desc, new String[] { SORT_ORDER_PUSHED, SORT_DIRECTION_DESC });
            SORT_LOOKUP.put(R.id.sort_updated_asc, new String[] { SORT_ORDER_UPDATED, SORT_DIRECTION_ASC });
            SORT_LOOKUP.put(R.id.sort_updated_desc, new String[] { SORT_ORDER_UPDATED, SORT_DIRECTION_DESC });
        }

        public SortDrawerHelper() {
        }

        public void setFilterType(String type) {
            mFilterType = type;
        }

        public int getMenuResId() {
            return TextUtils.equals(mFilterType, FILTER_TYPE_WATCHED) ? 0 : R.menu.repo_sort;
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