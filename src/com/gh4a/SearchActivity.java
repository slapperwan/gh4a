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
package com.gh4a;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.adapter.SearchRepositoryAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.utils.StringUtils;

public class SearchActivity extends BaseSherlockFragmentActivity {

    protected List<SearchRepository> repositories;
    protected UserAdapter userAdapter;
    protected SearchRepositoryAdapter repositoryAdapter;
    protected ListView mListViewResults;
    protected boolean mSearchByUser;// flag to search user or repo
    private ProgressDialog mProgressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.search);
        
        mListViewResults = (ListView) findViewById(R.id.list_search);
        registerForContextMenu(mListViewResults);

        final Spinner languageSpinner = (Spinner) findViewById(R.id.spinner_language);
        final EditText etSearchKey = (EditText) findViewById(R.id.et_search);
        ImageButton btnSearch = (ImageButton) findViewById(R.id.btn_search);
        if (Gh4Application.THEME == R.style.DefaultTheme) {
            btnSearch.setImageResource(R.drawable.action_search_dark);
        }

        /** event when user press enter button at soft keyboard */
        etSearchKey.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    String searchKey = etSearchKey.getText().toString();
                    String selectedLanguage = (String) languageSpinner.getSelectedItem();
                    mProgressDialog = SearchActivity.this.showProgressDialog(
                            SearchActivity.this.getResources().getString(R.string.loading_msg), true);
                    searchRepository(searchKey, selectedLanguage);
                    hideKeyboard(etSearchKey.getWindowToken());
                    return true;
                }
                return false;
            }
        });

        /** Event when user press button image */
        btnSearch.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                String searchKey = etSearchKey.getText().toString();
                String selectedLanguage = (String) languageSpinner.getSelectedItem();
                mProgressDialog = SearchActivity.this.showProgressDialog(
                        SearchActivity.this.getResources().getString(R.string.loading_msg), true);
                searchRepository(searchKey, selectedLanguage);
                hideKeyboard(etSearchKey.getWindowToken());
            }
        });
    }

    protected void searchRepository(final String searchKey, final String language) {
        mListViewResults.setOnItemClickListener(new OnRepositoryClickListener(this));
        repositories = new ArrayList<SearchRepository>();
        repositoryAdapter = new SearchRepositoryAdapter(this, repositories);
        mListViewResults.setAdapter(repositoryAdapter);
        new LoadRepositoryTask(this).execute(new String[] { searchKey, language, "true" });
    }

    protected void searchUser(final String searchKey) {
        mListViewResults.setOnItemClickListener(new OnUserClickListener(this));
        userAdapter = new UserAdapter(this, new ArrayList<User>(), R.layout.row_gravatar_1, false);
        mListViewResults.setAdapter(userAdapter);
        new LoadUserTask(this).execute(new String[] { searchKey });
    }

    protected List<User> getUsers(String searchKey) throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        UserService userService = new UserService();
        
        List<User> users = new ArrayList<User>();
//        if (!StringUtils.isBlank(searchKey)) {
//            users = service.searchUsersByName(searchKey);
//        }
//        else {
//            // TODO : show dialog
//        }
        return users;
    }

    private static class OnUserClickListener implements OnItemClickListener {

        private WeakReference<SearchActivity> mTarget;

        public OnUserClickListener(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (mTarget.get() != null) {
                User user = (User) adapterView.getAdapter().getItem(position);
                Intent intent = new Intent().setClass(mTarget.get(), UserActivity.class);
                intent.putExtra(Constants.User.USER_LOGIN, (String) user.getLogin());
                intent.putExtra(Constants.User.USER_NAME, (String) user.getName());
                mTarget.get().startActivity(intent);
            }
        }
    }

    protected void fillRepositoriesData() {
        if (repositories != null && repositories.size() > 0) {
            repositoryAdapter.notifyDataSetChanged();
            for (SearchRepository repository : repositories) {
                repositoryAdapter.add(repository);
            }
        }
        repositoryAdapter.notifyDataSetChanged();
    }

    protected void fillUsersData(List<User> users) {
        if (users != null && users.size() > 0) {
            for (User user : users) {
                userAdapter.add(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    protected List<SearchRepository> getRepositories(String searchKey, String language)
            throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        RepositoryService repoService = new RepositoryService();
        
        if (!StringUtils.isBlank(searchKey)) {
            if ("Any Language".equals(language)) {
                repositories = repoService.searchRepositories(searchKey, 1);
            }
            else {
                repositories = repoService.searchRepositories(searchKey, language, 1);
            }
        }
        else {
            // TODO : show dialog
        }
        return repositories;
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
                    activity.fillRepositoriesData();
                }
            }
        }
    }

    private static class LoadUserTask extends AsyncTask<String, Integer, List<User>> {

        private WeakReference<SearchActivity> mTarget;
        private boolean mException;

        public LoadUserTask(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        @Override
        protected List<User> doInBackground(String... params) {
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
        protected void onPostExecute(List<User> result) {
            if (mTarget.get() != null) {
                SearchActivity activity = mTarget.get();
                if (mException) {
                    mTarget.get().showError(false);
                }
                else {
                    activity.fillUsersData(result);
                }
            }
        }
    }

    private static class OnRepositoryClickListener implements OnItemClickListener {

        private WeakReference<SearchActivity> mTarget;

        public OnRepositoryClickListener(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (mTarget.get() != null) {
                SearchRepository repository = (SearchRepository) adapterView.getAdapter().getItem(position);
                mTarget.get().getApplicationContext().openRepositoryInfoActivity(mTarget.get(),
                        repository.getOwner(), repository.getName(), 0);
            }
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.clear();// clear items

        if (v.getId() == R.id.list_search) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            menu.setHeaderTitle("Go to");

            /** Menu for user */
            if (mSearchByUser) {
                User user = (User) userAdapter.getItem(info.position);
                menu.add("User " + user.getLogin()
                        + (!StringUtils.isBlank(user.getName()) ? " - " + user.getName() : ""));
            }

            /** Menu for repository */
            else {
                SearchRepository repository = (SearchRepository) repositoryAdapter.getItem(info.position);
                menu.add("User " + repository.getOwner());
                menu.add("Repo " + repository.getName());
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item
                .getMenuInfo();

        ListAdapter listAdapter = null;
        if (mSearchByUser) {
            listAdapter = userAdapter;
        }
        else {
            listAdapter = repositoryAdapter;
        }

        Object object = (Object) listAdapter.getItem(info.position);

        String title = item.getTitle().toString();

        /** User item */
        if (title.startsWith("User")) {
            Intent intent = new Intent().setClass(SearchActivity.this, UserActivity.class);

            String username = null;
            if (object instanceof SearchRepository) {
                SearchRepository repository = (SearchRepository) object;
                username = repository.getOwner();
            }
            if (mSearchByUser) {
                User user = (User) object;
                username = user.getLogin();
            }

            intent.putExtra(Constants.User.USER_LOGIN, username);
            startActivity(intent);
        }
        /** Repo item */
        else if (title.startsWith("Repo")) {
            SearchRepository repository = (SearchRepository) object;
            getApplicationContext().openRepositoryInfoActivity(this,
                    repository.getOwner(), repository.getName(), 0);
        }
        return true;
    }
}