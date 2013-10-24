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
import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.SearchUser;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.widget.SearchView;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.adapter.SearchRepositoryAdapter;
import com.gh4a.adapter.SearchUserAdapter;
import com.gh4a.utils.StringUtils;

public class SearchActivity extends BaseSherlockFragmentActivity implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {

    protected SearchUserAdapter mUserAdapter;
    protected SearchRepositoryAdapter mRepoAdapter;
    protected ListView mListViewResults;
    private ProgressDialog mProgressDialog;

    private Spinner mSearchType;
    private SearchView mSearch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.search);

        LinearLayout searchLayout = (LinearLayout) getLayoutInflater().inflate(
                R.layout.search_action_bar, null);
        actionBar.setCustomView(searchLayout);
        actionBar.setDisplayShowCustomEnabled(true);

        mSearchType = (Spinner) searchLayout.findViewById(R.id.search_type);
        mSearchType.setAdapter(new SearchTypeAdapter(this,
                Gh4Application.THEME == R.style.LightTheme));
        mSearchType.setOnItemSelectedListener(this);

        mSearch = (SearchView) searchLayout.findViewById(R.id.search_view);
        mSearch.setIconifiedByDefault(true);
        mSearch.requestFocus();
        mSearch.setIconified(false);
        mSearch.setOnQueryTextListener(this);
        mSearch.setOnCloseListener(this);
        mSearch.onActionViewExpanded();

        updateSearchTypeHint();

        mListViewResults = (ListView) findViewById(R.id.list_search);
        mListViewResults.setOnItemClickListener(this);
        registerForContextMenu(mListViewResults);
    }

    protected void searchRepository(final String searchKey, final String language) {
        mRepoAdapter = new SearchRepositoryAdapter(this);
        mListViewResults.setAdapter(mRepoAdapter);
        new LoadRepositoryTask(this).execute(new String[] { searchKey, language, "true" });
    }

    protected void searchUser(final String searchKey) {
        mUserAdapter = new SearchUserAdapter(this);
        mListViewResults.setAdapter(mUserAdapter);
        new LoadUserTask(this).execute(new String[] { searchKey });
    }

    protected List<SearchUser> getUsers(String searchKey) throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        UserService userService = new UserService();
        
        if (StringUtils.isBlank(searchKey)) {
            return null;
        }

        return userService.searchUsers(searchKey);
    }

    private static class SearchTypeAdapter extends BaseAdapter implements SpinnerAdapter {
        private Context mContext;
        private boolean mLightTheme;

        private static final int[][] RESOURCES = new int[][] {
            { R.string.search_type_repo, R.drawable.search_repos, R.drawable.search_repos_dark },
            { R.string.search_type_user, R.drawable.search_users, R.drawable.search_users_dark }
        };

        SearchTypeAdapter(Context context, boolean lightTheme) {
            mContext = context;
            mLightTheme = lightTheme;
        }

        @Override
        public int getCount() {
            return RESOURCES.length;
        }

        @Override
        public CharSequence getItem(int position) {
            return mContext.getString(RESOURCES[position][0]);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.search_type_small, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(RESOURCES[position][mLightTheme ? 1 : 2]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(mContext);
                convertView = inflater.inflate(R.layout.search_type_popup, null);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            icon.setImageResource(RESOURCES[position][mLightTheme ? 1 : 2]);

            TextView label = (TextView) convertView.findViewById(R.id.label);
            label.setText(RESOURCES[position][0]);

            return convertView;
        }
    }

    protected void fillRepositoriesData(List<SearchRepository> repos) {
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

    protected List<SearchRepository> getRepositories(String searchKey, String language)
            throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        RepositoryService repoService = new RepositoryService();
        
        if (StringUtils.isBlank(searchKey)) {
            return null;
        }

        if (language == null || getResources().getStringArray(R.array.languages_array)[0].equals(language)) {
            return repoService.searchRepositories(searchKey, 1);
        }
        else {
            return repoService.searchRepositories(searchKey, language, 1);
        }
    }

    private static class LoadRepositoryTask extends AsyncTask<String, Integer, List<SearchRepository>> {

        private boolean mException;
        private WeakReference<SearchActivity> mTarget;

        public LoadRepositoryTask(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        @Override
        protected List<SearchRepository> doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getRepositories(params[0], params[1]);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<SearchRepository> result) {
            if (mTarget.get() != null) {
                SearchActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                if (mException) {
                    mTarget.get().showError(false);
                }
                else {
                    activity.fillRepositoriesData(result);
                }
            }
        }
    }

    private static class LoadUserTask extends AsyncTask<String, Integer, List<SearchUser>> {

        private WeakReference<SearchActivity> mTarget;
        private boolean mException;

        public LoadUserTask(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        @Override
        protected List<SearchUser> doInBackground(String... params) {
            if (mTarget.get() != null) {
                try {
                    return mTarget.get().getUsers(params[0]);
                }
                catch (IOException e) {
                    Log.e(Constants.LOG_TAG, e.getMessage(), e);
                    mException = true;
                    return null;
                }
            }
            else {
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(List<SearchUser> result) {
            if (mTarget.get() != null) {
                SearchActivity activity = mTarget.get();
                activity.stopProgressDialog(activity.mProgressDialog);
                if (mException) {
                    mTarget.get().showError(false);
                }
                else {
                    activity.fillUsersData(result);
                }
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.clear();// clear items

        if (v.getId() == R.id.list_search) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            ListAdapter listAdapter = mListViewResults.getAdapter();
            Object object = listAdapter.getItem(info.position);
            menu.setHeaderTitle(R.string.go_to);

            /** Menu for user */
            if (object instanceof SearchUser) {
                SearchUser user = (SearchUser) object;
                menu.add(getString(R.string.menu_user, StringUtils.formatName(user.getLogin(), user.getName())));
            }

            /** Menu for repository */
            else {
                SearchRepository repository = (SearchRepository) object;
                menu.add(getString(R.string.menu_user, repository.getOwner()));
                menu.add(getString(R.string.menu_repo, repository.getName()));
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        ListAdapter listAdapter = mListViewResults.getAdapter();
        Object object = listAdapter.getItem(info.position);

        /** User item */
        if (object instanceof SearchUser) {
            Intent intent = new Intent().setClass(SearchActivity.this, UserActivity.class);
            SearchUser user = (SearchUser) object;

            intent.putExtra(Constants.User.USER_LOGIN, user.getLogin());
            startActivity(intent);
        }
        /** Repo item */
        else {
            SearchRepository repository = (SearchRepository) object;
            Gh4Application.get(this).openRepositoryInfoActivity(this,
                    repository.getOwner(), repository.getName(), 0);
        }
        return true;
    }

    private void updateSearchTypeHint() {
        switch (mSearchType.getSelectedItemPosition()) {
            case 0: mSearch.setQueryHint(getString(R.string.search_hint_repo)); break;
            case 1: mSearch.setQueryHint(getString(R.string.search_hint_user)); break;
            default: mSearch.setQueryHint(null); break;
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        boolean searchUser = mSearchType.getSelectedItemPosition() == 1;
        mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
        if (searchUser) {
            searchUser(query);
        } else {
            searchRepository(query, null);
        }
        mSearch.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateSearchTypeHint();
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
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object object = parent.getAdapter().getItem(position);

        if (object instanceof SearchUser) {
            SearchUser user = (SearchUser) object;
            Intent intent = new Intent(this, UserActivity.class);
            intent.putExtra(Constants.User.USER_LOGIN, (String) user.getLogin());
            intent.putExtra(Constants.User.USER_NAME, (String) user.getName());
            startActivity(intent);
        } else if (object instanceof SearchRepository) {
            SearchRepository repository = (SearchRepository) object;
            Gh4Application.get(this).openRepositoryInfoActivity(this,
                    repository.getOwner(), repository.getName(), 0);
        }
    }
}