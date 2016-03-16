/*
 * Copyright 2011 Azwan Adli Abdullah
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gh4a.activities;

import java.util.List;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.client.RequestException;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.gh4a.BaseActivity;
import com.gh4a.R;
import com.gh4a.adapter.CodeSearchAdapter;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.SearchUserAdapter;
import com.gh4a.loader.CodeSearchLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositorySearchLoader;
import com.gh4a.loader.UserSearchLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;

public class SearchActivity extends BaseActivity implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        AdapterView.OnItemSelectedListener, RootAdapter.OnItemClickListener {

    private RootAdapter<?, ?> mAdapter;
    private RecyclerView mResultsView;

    private Spinner mSearchType;
    private SearchView mSearch;
    private String mQuery;

    private static final String STATE_KEY_QUERY = "query";
    private static final String STATE_KEY_SEARCH_MODE = "search_mode";
    private static final int SEARCH_MODE_NONE = 0;
    private static final int SEARCH_MODE_REPO = 1;
    private static final int SEARCH_MODE_USER = 2;
    private static final int SEARCH_MODE_CODE = 3;

    private LoaderCallbacks<List<Repository>> mRepoCallback = new LoaderCallbacks<List<Repository>>(this) {
        @Override
        protected Loader<LoaderResult<List<Repository>>> onCreateLoader() {
            RepositorySearchLoader loader = new RepositorySearchLoader(SearchActivity.this, null);
            loader.setQuery(mQuery);
            return loader;
        }

        @Override
        protected void onResultReady(List<Repository> result) {
            fillRepositoriesData(result);
        }
    };

    private LoaderCallbacks<List<SearchUser>> mUserCallback = new LoaderCallbacks<List<SearchUser>>(this) {
        @Override
        protected Loader<LoaderResult<List<SearchUser>>> onCreateLoader() {
            return new UserSearchLoader(SearchActivity.this, mQuery);
        }

        @Override
        protected void onResultReady(List<SearchUser> result) {
            fillUsersData(result);
        }
    };

    private LoaderCallbacks<List<CodeSearchResult>> mCodeCallback = new LoaderCallbacks<List<CodeSearchResult>>(this) {
        @Override
        protected Loader<LoaderResult<List<CodeSearchResult>>> onCreateLoader() {
            return new CodeSearchLoader(SearchActivity.this, mQuery);
        }

        @Override
        protected void onResultReady(List<CodeSearchResult> result) {
            fillCodeData(result);
        }

        @Override
        protected boolean onError(Exception e) {
             if (e instanceof RequestException) {
                RequestError error = ((RequestException) e).getError();
                if (error != null && error.getErrors() != null && !error.getErrors().isEmpty()) {
                    setEmptyText(R.string.code_search_too_broad);
                    setContentEmpty(true);
                    setContentShown(false);
                    return true;
                }
            }
            return super.onError(e);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.search);

        LayoutInflater inflater = LayoutInflater.from(UiUtils.makeHeaderThemedContext(this));
        LinearLayout searchLayout = (LinearLayout) inflater.inflate(R.layout.search_action_bar, null);
        actionBar.setCustomView(searchLayout);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mSearchType = (Spinner) searchLayout.findViewById(R.id.search_type);
        mSearchType.setAdapter(new SearchTypeAdapter(actionBar.getThemedContext(), this));
        mSearchType.setOnItemSelectedListener(this);

        mSearch = (SearchView) searchLayout.findViewById(R.id.search_view);
        mSearch.setIconifiedByDefault(true);
        mSearch.requestFocus();
        mSearch.setIconified(false);
        mSearch.setOnQueryTextListener(this);
        mSearch.setOnCloseListener(this);
        mSearch.onActionViewExpanded();

        updateSelectedSearchType();

        mResultsView = (RecyclerView) findViewById(R.id.list);
        mResultsView.setLayoutManager(new LinearLayoutManager(this));
        mResultsView.addItemDecoration(new DividerItemDecoration(this));

        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_KEY_QUERY);
            mSearch.setQuery(mQuery, false);

            LoaderManager lm = getSupportLoaderManager();
            int previousMode = savedInstanceState.getInt(STATE_KEY_SEARCH_MODE, SEARCH_MODE_NONE);
            switch (previousMode) {
                case SEARCH_MODE_REPO: lm.initLoader(0, null, mRepoCallback); break;
                case SEARCH_MODE_USER: lm.initLoader(0, null, mUserCallback); break;
                case SEARCH_MODE_CODE: lm.initLoader(0, null, mCodeCallback); break;
            }
        }
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // User can resubmit the query to restart the search
        return false;
    }

    @Override
    public void onRefresh() {
        Loader loader = getSupportLoaderManager().getLoader(0);
        if (loader != null) {
            if (mAdapter != null) {
                mAdapter.clear();
            }
            loader.onContentChanged();
        }
        super.onRefresh();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mAdapter instanceof RepositoryAdapter) {
            outState.putInt(STATE_KEY_SEARCH_MODE, SEARCH_MODE_REPO);
        } else if (mAdapter instanceof SearchUserAdapter) {
            outState.putInt(STATE_KEY_SEARCH_MODE, SEARCH_MODE_USER);
        } else if (mAdapter instanceof CodeSearchAdapter) {
            outState.putInt(STATE_KEY_SEARCH_MODE, SEARCH_MODE_CODE);
        }
        outState.putString(STATE_KEY_QUERY, mQuery);
        super.onSaveInstanceState(outState);
    }

    private static class SearchTypeAdapter extends BaseAdapter implements SpinnerAdapter {
        private Context mContext;
        private LayoutInflater mInflater;
        private LayoutInflater mPopupInflater;

        private final int[][] mResources = new int[][] {
            { R.string.search_type_repo, R.drawable.search_repos_dark, R.attr.searchRepoIcon, 0 },
            { R.string.search_type_user, R.drawable.search_users_dark, R.attr.searchUserIcon, 0 },
            { R.string.search_type_code, R.drawable.search_code_dark, R.attr.searchCodeIcon, 0 }
        };

        SearchTypeAdapter(Context context, Context popupContext) {
            mContext = context;
            mInflater = LayoutInflater.from(context);
            mPopupInflater = LayoutInflater.from(popupContext);
            for (int i = 0; i < mResources.length; i++) {
                mResources[i][3] = UiUtils.resolveDrawable(popupContext, mResources[i][2]);
            }
        }

        @Override
        public int getCount() {
            return mResources.length;
        }

        @Override
        public CharSequence getItem(int position) {
            return mContext.getString(mResources[position][0]);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.search_type_small, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][1]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mPopupInflater.inflate(R.layout.search_type_popup, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][3]);

            TextView label = (TextView) convertView.findViewById(R.id.label);
            label.setText(mResources[position][0]);

            return convertView;
        }
    }

    protected void fillRepositoriesData(List<Repository> repos) {
        RepositoryAdapter adapter = new RepositoryAdapter(this);
        adapter.addAll(repos);
        setAdapter(adapter);
    }

    protected void fillUsersData(List<SearchUser> users) {
        SearchUserAdapter adapter = new SearchUserAdapter(this);
        adapter.addAll(users);
        setAdapter(adapter);
    }

    protected void fillCodeData(List<CodeSearchResult> results) {
        CodeSearchAdapter adapter = new CodeSearchAdapter(this);
        adapter.addAll(results);
        setAdapter(adapter);
    }

    private void setAdapter(RootAdapter<?, ?> adapter) {
        adapter.setOnItemClickListener(this);
        mResultsView.setAdapter(adapter);
        mAdapter = adapter;
        setContentShown(true);
        setContentEmpty(adapter.getCount() == 0);
    }

    private void updateSelectedSearchType() {
        switch (mSearchType.getSelectedItemPosition()) {
            case 0:
                mSearch.setQueryHint(getString(R.string.search_hint_repo));
                setEmptyText(R.string.no_search_repos_found);
                break;
            case 1:
                mSearch.setQueryHint(getString(R.string.search_hint_user));
                setEmptyText(R.string.no_search_users_found);
                break;
            case 2:
                mSearch.setQueryHint(getString(R.string.search_hint_code));
                setEmptyText(R.string.no_search_code_found);
                break;
            default:
                mSearch.setQueryHint(null);
                setEmptyText(null);
                break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        LoaderManager lm = getSupportLoaderManager();
        switch (mSearchType.getSelectedItemPosition()) {
            case 1: lm.restartLoader(0, null, mUserCallback); break;
            case 2: lm.restartLoader(0, null, mCodeCallback); break;
            default: lm.restartLoader(0, null, mRepoCallback); break;
        }
        setContentShown(false);
        mSearch.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQuery = newText;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateSelectedSearchType();
        if (getSupportLoaderManager().getLoader(0) != null) {
            onQueryTextSubmit(mQuery);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        updateSelectedSearchType();
    }

    @Override
    public boolean onClose() {
        if (mAdapter != null) {
            mAdapter.clear();
        }
        mQuery = null;
        return true;
    }

    @Override
    public void onItemClick(Object item) {
        if (item instanceof Repository) {
            Repository repository = (Repository) item;
            startActivity(IntentUtils.getRepoActivityIntent(this,
                    repository.getOwner().getLogin(), repository.getName(), null));
        } else if (item instanceof CodeSearchResult) {
            CodeSearchResult result = (CodeSearchResult) item;
            Repository repo = result.getRepository();
            Uri uri = Uri.parse(result.getUrl());
            String ref = uri.getQueryParameter("ref");
            startActivity(IntentUtils.getFileViewerActivityIntent(this,
                    repo.getOwner().getLogin(), repo.getName(), ref, result.getPath()));
        } else {
            SearchUser user = (SearchUser) item;
            startActivity(IntentUtils.getUserActivityIntent(this, user.getLogin(), user.getName()));
        }
    }
}