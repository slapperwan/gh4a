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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.egit.github.core.CodeSearchResult;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
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
import android.widget.Toast;

import com.gh4a.BaseActivity;
import com.gh4a.Gh4Application;
import com.gh4a.ProgressDialogTask;
import com.gh4a.R;
import com.gh4a.adapter.CodeSearchAdapter;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.SearchUserAdapter;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.gh4a.widget.DividerItemDecoration;

public class SearchActivity extends BaseActivity implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        AdapterView.OnItemSelectedListener, RootAdapter.OnItemClickListener {

    private SearchUserAdapter mUserAdapter;
    private RepositoryAdapter mRepoAdapter;
    private CodeSearchAdapter mCodeAdapter;
    private RecyclerView mResultsView;

    private Spinner mSearchType;
    private SearchView mSearch;
    private String mQuery;
    private boolean mSubmitted;

    private static final String STATE_KEY_REPO_RESULTS = "repo_results";
    private static final String STATE_KEY_USER_RESULTS = "user_results";
    private static final String STATE_KEY_CODE_RESULTS = "code_results";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

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

        updateSearchTypeHint();

        mResultsView = (RecyclerView) findViewById(R.id.list);
        mResultsView.setLayoutManager(new LinearLayoutManager(this));
        mResultsView.addItemDecoration(new DividerItemDecoration(this));
        registerForContextMenu(mResultsView);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(STATE_KEY_REPO_RESULTS)) {
                mRepoAdapter = new RepositoryAdapter(this);
                mRepoAdapter.setOnItemClickListener(this);
                mResultsView.setAdapter(mRepoAdapter);
                ArrayList<Repository> data =(ArrayList<Repository>)
                        savedInstanceState.getSerializable(STATE_KEY_REPO_RESULTS);
                fillRepositoriesData(data);
            } else if (savedInstanceState.containsKey(STATE_KEY_USER_RESULTS)) {
                mUserAdapter = new SearchUserAdapter(this);
                mUserAdapter.setOnItemClickListener(this);
                mResultsView.setAdapter(mUserAdapter);
                ArrayList<SearchUser> data =(ArrayList<SearchUser>)
                        savedInstanceState.getSerializable(STATE_KEY_USER_RESULTS);
                fillUsersData(data);
            } else if (savedInstanceState.containsKey(STATE_KEY_CODE_RESULTS)) {
                mCodeAdapter = new CodeSearchAdapter(this);
                mCodeAdapter.setOnItemClickListener(this);
                mResultsView.setAdapter(mCodeAdapter);
                ArrayList<CodeSearchResult> data =(ArrayList<CodeSearchResult>)
                        savedInstanceState.getSerializable(STATE_KEY_CODE_RESULTS);
                fillCodeData(data);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRepoAdapter != null) {
            int count = mRepoAdapter.getCount();
            ArrayList<Repository> repos = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                repos.add(mRepoAdapter.getItem(i));
            }
            outState.putSerializable(STATE_KEY_REPO_RESULTS, repos);
        } else if (mUserAdapter != null) {
            int count = mUserAdapter.getCount();
            ArrayList<SearchUser> users = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                users.add(mUserAdapter.getItem(i));
            }
            outState.putSerializable(STATE_KEY_USER_RESULTS, users);
        } else if (mCodeAdapter != null) {
            int count = mCodeAdapter.getCount();
            ArrayList<CodeSearchResult> users = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                users.add(mCodeAdapter.getItem(i));
            }
            outState.putSerializable(STATE_KEY_CODE_RESULTS, users);
        }
    }

    protected void searchRepository(final String searchKey) {
        mRepoAdapter = new RepositoryAdapter(this);
        mRepoAdapter.setOnItemClickListener(this);
        mResultsView.setAdapter(mRepoAdapter);
        AsyncTaskCompat.executeParallel(new LoadRepositoryTask(searchKey));
    }

    protected void searchUser(final String searchKey) {
        mUserAdapter = new SearchUserAdapter(this);
        mUserAdapter.setOnItemClickListener(this);
        mResultsView.setAdapter(mUserAdapter);
        AsyncTaskCompat.executeParallel(new LoadUserTask(searchKey));
    }

    protected void searchCode(final String searchKey) {
        mCodeAdapter = new CodeSearchAdapter(this);
        mCodeAdapter.setOnItemClickListener(this);
        mResultsView.setAdapter(mCodeAdapter);
        AsyncTaskCompat.executeParallel(new SearchCodeTask(searchKey));

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
        if (mRepoAdapter != null) {
            mRepoAdapter.clear();
            if (repos != null) {
                mRepoAdapter.addAll(repos);
            }
            mRepoAdapter.notifyDataSetChanged();
        }
    }

    protected void fillUsersData(List<SearchUser> users) {
        if (mUserAdapter != null) {
            mUserAdapter.clear();
            if (users != null) {
                mUserAdapter.addAll(users);
            }
            mUserAdapter.notifyDataSetChanged();
        }
    }

    protected void fillCodeData(List<CodeSearchResult> results) {
        if (mCodeAdapter != null) {
            mCodeAdapter.clear();
            if (results != null) {
                mCodeAdapter.addAll(results);
            }
            mCodeAdapter.notifyDataSetChanged();
        }
    }

    // TODO: replace this by using loaders (would avoid the need for manually
    //       saving the results into the saved instance state)
    private class LoadRepositoryTask extends ProgressDialogTask<List<Repository>> {
        private String mQuery;

        public LoadRepositoryTask(String query) {
            super(SearchActivity.this, 0, R.string.loading_msg);
            mQuery = query;
        }

        @Override
        protected List<Repository> run() throws IOException {
            if (StringUtils.isBlank(mQuery)) {
                return null;
            }

            RepositoryService repoService = (RepositoryService)
                    Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
            HashMap<String, String> params = new HashMap<>();
            params.put("fork", "true");

            return repoService.searchRepositories(mQuery, params);
        }

        @Override
        protected void onSuccess(List<Repository> result) {
            fillRepositoriesData(result);
        }
    }

    private class LoadUserTask extends ProgressDialogTask<List<SearchUser>> {
        private String mQuery;

        public LoadUserTask(String query) {
            super(SearchActivity.this, 0, R.string.loading_msg);
            mQuery = query;
        }

        @Override
        protected List<SearchUser> run() throws IOException {
            if (StringUtils.isBlank(mQuery)) {
                return null;
            }

            UserService userService = (UserService)
                    Gh4Application.get().getService(Gh4Application.USER_SERVICE);
            return userService.searchUsers(mQuery);
        }

        @Override
        protected void onSuccess(List<SearchUser> result) {
            fillUsersData(result);
        }
    }

    private class SearchCodeTask extends ProgressDialogTask<List<CodeSearchResult>> {
        private String mQuery;

        public SearchCodeTask(String query) {
            super(SearchActivity.this, 0, R.string.loading_msg);
            mQuery = query;
        }

        @Override
        protected List<CodeSearchResult> run() throws IOException {
            if (StringUtils.isBlank(mQuery)) {
                return null;
            }

            RepositoryService repoService = (RepositoryService)
                    Gh4Application.get().getService(Gh4Application.REPO_SERVICE);
            return repoService.searchCode(mQuery);
        }

        @Override
        protected void onSuccess(List<CodeSearchResult> result) {
            fillCodeData(result);
        }

        @Override
        protected void onError(Exception e) {
            if (e instanceof RequestException) {
                RequestError error = ((RequestException) e).getError();
                if (error != null && error.getErrors() != null && !error.getErrors().isEmpty()) {
                    Toast.makeText(SearchActivity.this,
                            R.string.code_search_too_broad_toast, Toast.LENGTH_LONG).show();
                    return;
                }
            }
            super.onError(e);
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.clear();// clear items

        if (v.getId() == android.R.id.list) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            RootAdapter<?, ?> adapter = (RootAdapter<?, ?>) mResultsView.getAdapter();
            Object object = adapter.getItem(info.position);
            menu.setHeaderTitle(R.string.go_to);

            if (object instanceof SearchUser) {
                /** Menu for user */
                SearchUser user = (SearchUser) object;
                menu.add(getString(R.string.menu_user, StringUtils.formatName(user.getLogin(), user.getName())))
                        .setIntent(IntentUtils.getUserActivityIntent(this, user.getLogin(), user.getName()));
            } else {
                /** Menu for repository */
                Repository repository = (Repository) object;
                User owner = repository.getOwner();
                menu.add(getString(R.string.menu_user, StringUtils.formatName(owner.getLogin(), owner.getName())))
                        .setIntent(IntentUtils.getUserActivityIntent(this, owner.getLogin(), owner.getName()));
                menu.add(getString(R.string.menu_repo, repository.getName()))
                        .setIntent(IntentUtils.getRepoActivityIntent(this, owner.getLogin(), repository.getName(), null));
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    private void updateSearchTypeHint() {
        switch (mSearchType.getSelectedItemPosition()) {
            case 0: mSearch.setQueryHint(getString(R.string.search_hint_repo)); break;
            case 1: mSearch.setQueryHint(getString(R.string.search_hint_user)); break;
            case 2: mSearch.setQueryHint(getString(R.string.search_hint_code)); break;
            default: mSearch.setQueryHint(null); break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        switch (mSearchType.getSelectedItemPosition()) {
            case 1: searchUser(query); break;
            case 2: searchCode(query); break;
            default: searchRepository(query); break;
        }
        mSubmitted = true;
        mSearch.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQuery = newText;
        mSubmitted = false;
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateSearchTypeHint();
        if (mSubmitted) {
            onQueryTextSubmit(mQuery);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        updateSearchTypeHint();
    }

    @Override
    public boolean onClose() {
        if (mUserAdapter != null) {
            mUserAdapter.clear();
            mUserAdapter.notifyDataSetChanged();
        }
        if (mRepoAdapter != null) {
            mRepoAdapter.clear();
            mRepoAdapter.notifyDataSetChanged();
        }
        if (mCodeAdapter != null) {
            mCodeAdapter.clear();
            mCodeAdapter.notifyDataSetChanged();
        }
        mQuery = null;
        mSubmitted = false;
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