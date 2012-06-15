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

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.SearchRepository;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

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
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.gh4a.adapter.SearchRepositoryAdapter;
import com.gh4a.adapter.UserAdapter;
import com.gh4a.utils.StringUtils;

/**
 * The Search activity.
 */
public class SearchActivity extends BaseActivity {

    /** The loading dialog. */
    protected LoadingDialog mLoadingDialog;

    /** The repositories. */
    protected List<SearchRepository> repositories;

    /** The user adapter. */
    protected UserAdapter userAdapter;

    /** The repository adapter. */
    protected SearchRepositoryAdapter repositoryAdapter;

    /** The list view results. */
    protected ListView mListViewResults;

    /** The reloading. */
    protected boolean mLoading;

    /** The reload. */
    protected boolean mReload;

    /** The first time search. */
    protected boolean mFirstTimeSearch;

    /** The page. */
    protected int mPage = 1;

    /** The search by user. */
    protected boolean mSearchByUser;// flag to search user or repo

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.search);
        setUpActionBar();

        mListViewResults = (ListView) findViewById(R.id.list_search);
        registerForContextMenu(mListViewResults);

        final Spinner languageSpinner = (Spinner) findViewById(R.id.spinner_language);
        final Spinner searchTypeSpinner = (Spinner) findViewById(R.id.spinner_search_type);
        final EditText etSearchKey = (EditText) findViewById(R.id.et_search);
        ImageButton btnSearch = (ImageButton) findViewById(R.id.btn_search);

        /** event when user press enter button at soft keyboard */
        etSearchKey.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    mFirstTimeSearch = true;
                    mPage = 1;// reset to 1;
                    String searchKey = etSearchKey.getText().toString();
                    String selectedLanguage = (String) languageSpinner.getSelectedItem();
                    if (searchTypeSpinner.getSelectedItemPosition() == 1) {
                        mSearchByUser = true;
                        searchUser(searchKey);
                    }
                    else {
                        mSearchByUser = false;
                        searchRepository(searchKey, selectedLanguage);
                    }

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
                mFirstTimeSearch = true;
                mPage = 1;// reset to 1;
                String searchKey = etSearchKey.getText().toString();
                String selectedLanguage = (String) languageSpinner.getSelectedItem();
                if (searchTypeSpinner.getSelectedItemPosition() == 1) {
                    mSearchByUser = true;
                    searchUser(searchKey);
                }
                else {
                    mSearchByUser = false;
                    searchRepository(searchKey, selectedLanguage);
                }

                hideKeyboard(etSearchKey.getWindowToken());
            }
        });
    }

    /**
     * Search repository.
     * 
     * @param searchKey the search key
     * @param language the language
     */
    protected void searchRepository(final String searchKey, final String language) {
        mListViewResults.setOnItemClickListener(new OnRepositoryClickListener(this));

        repositories = new ArrayList<SearchRepository>();
        repositoryAdapter = new SearchRepositoryAdapter(this, repositories, R.layout.row_simple_3);
        mListViewResults.setAdapter(repositoryAdapter);
        mListViewResults
                .setOnScrollListener(new RepositoryScrollListener(this, searchKey, language));

        new LoadRepositoryTask(this).execute(new String[] { searchKey, language, "true" });
    }

    /**
     * Search user.
     * 
     * @param searchKey the search key
     */
    protected void searchUser(final String searchKey) {
        mListViewResults.setOnItemClickListener(new OnUserClickListener(this));
        mListViewResults.setOnScrollListener(null);// reset listener as the API
                                                   // doesn't have the
                                                   // pagination

        userAdapter = new UserAdapter(this, new ArrayList<User>(), R.layout.row_gravatar_1, true);
        mListViewResults.setAdapter(userAdapter);

        new LoadUserTask(this).execute(new String[] { searchKey });
    }

    /**
     * Gets the users.
     * 
     * @param searchKey the search key
     * @return the users
     */
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

    /**
     * Callback to be invoked when user in the AdapterView has been clicked.
     */
    private static class OnUserClickListener implements OnItemClickListener {

        /** The target. */
        private WeakReference<SearchActivity> mTarget;

        /**
         * Instantiates a new on user click listener.
         *
         * @param activity the activity
         */
        public OnUserClickListener(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AdapterView.OnItemClickListener#onItemClick(android
         * .widget.AdapterView, android.view.View, int, long)
         */
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

    /**
     * Fill repositories data to UI.
     */
    protected void fillRepositoriesData() {
        if (repositories != null && repositories.size() > 0) {
            repositoryAdapter.notifyDataSetChanged();
            for (SearchRepository repository : repositories) {
                repositoryAdapter.add(repository);
            }
        }
        repositoryAdapter.notifyDataSetChanged();
    }

    /**
     * Fill users data to UI.
     * 
     * @param users the users
     */
    protected void fillUsersData(List<User> users) {
        if (users != null && users.size() > 0) {
            for (User user : users) {
                userAdapter.add(user);
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    /**
     * Callback to be invoked when the list/grid of Repository has been
     * scrolled.
     */
    private static class RepositoryScrollListener implements OnScrollListener {

        /** The search key. */
        private String searchKey;

        /** The language. */
        private String language;

        /** The target. */
        private WeakReference<SearchActivity> mTarget;

        /**
         * Instantiates a new repository scoll listener.
         *
         * @param activity the activity
         * @param searchKey the search key
         * @param language the language
         */
        public RepositoryScrollListener(SearchActivity activity, String searchKey, String language) {
            super();
            this.searchKey = searchKey;
            this.language = language;
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScrollStateChanged(
         * android.widget.AbsListView, int)
         */
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mTarget.get() != null) {
                if (mTarget.get().mReload && scrollState == SCROLL_STATE_IDLE) {
                    new LoadRepositoryTask(mTarget.get()).execute(new String[] { searchKey, language,
                            "false" });
                    mTarget.get().mReload = false;
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AbsListView.OnScrollListener#onScroll(android.widget
         * .AbsListView, int, int, int)
         */
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                int totalItemCount) {
            if (mTarget.get() != null) {
                if (!mTarget.get().mLoading && firstVisibleItem != 0
                        && ((firstVisibleItem + visibleItemCount) == totalItemCount)) {
                    mTarget.get().mReload = true;
                    mTarget.get().mLoading = true;
                }
            }
        }
    }

    /**
     * Gets the repositories.
     *
     * @param searchKey the search key
     * @param language the language
     * @return the repositories
     * @throws GitHubException the git hub exception
     */
    protected List<SearchRepository> getRepositories(String searchKey, String language)
            throws IOException {
        GitHubClient client = new GitHubClient();
        client.setOAuth2Token(getAuthToken());
        RepositoryService repoService = new RepositoryService();
        
        if (!StringUtils.isBlank(searchKey)) {
            if ("Any Language".equals(language)) {
                repositories = repoService.searchRepositories(searchKey, mPage);
            }
            else {
                repositories = repoService.searchRepositories(searchKey, language, mPage);
            }
            mPage++;
        }
        else {
            // TODO : show dialog
        }
        return repositories;
    }

    /**
     * An asynchronous task that runs on a background thread to load repository.
     */
    private static class LoadRepositoryTask extends AsyncTask<String, Integer, List<SearchRepository>> {

        /** The hide main view. */
        private boolean mHideMainView;
        
        /** The exception. */
        private boolean mException;

        /** The target. */
        private WeakReference<SearchActivity> mTarget;

        /**
         * Instantiates a new load repository task.
         *
         * @param activity the activity
         */
        public LoadRepositoryTask(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
        @Override
        protected List<SearchRepository> doInBackground(String... params) {
            if (mTarget.get() != null) {
                this.mHideMainView = Boolean.valueOf(params[2]);
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                if (mTarget.get().mPage == 1) {
                    mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true,
                            mHideMainView);
                }
                else {
                    TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                    loadingView.setVisibility(View.VISIBLE);
                }
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
        @Override
        protected void onPostExecute(List<SearchRepository> result) {
            if (mTarget.get() != null) {
                SearchActivity activity = mTarget.get();
                if (mException) {
                    mTarget.get().showError(false);
                }
                else {
                    activity.fillRepositoriesData();
                }
    
                if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                    activity.mLoadingDialog.dismiss();
                }
    
                TextView loadingView = (TextView) mTarget.get().findViewById(R.id.tv_loading);
                loadingView.setVisibility(View.GONE);
    
                activity.mFirstTimeSearch = false;
                activity.mLoading = false;
            }
        }
    }

    /**
     * An asynchronous task that runs on a background thread to load user.
     */
    private static class LoadUserTask extends AsyncTask<String, Integer, List<User>> {

        /** The target. */
        private WeakReference<SearchActivity> mTarget;
        
        /** The exception. */
        private boolean mException;

        /**
         * Instantiates a new load user task.
         *
         * @param activity the activity
         */
        public LoadUserTask(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#doInBackground(Params[])
         */
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

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPreExecute()
         */
        @Override
        protected void onPreExecute() {
            if (mTarget.get() != null) {
                mTarget.get().mLoadingDialog = LoadingDialog.show(mTarget.get(), true, true);
            }
        }

        /*
         * (non-Javadoc)
         * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
         */
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
    
                if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                    activity.mLoadingDialog.dismiss();
                }
                
                activity.mFirstTimeSearch = false;
            }
        }
    }

    /**
     * Callback to be invoked when repository in the AdapterView has been
     * clicked.
     */
    private static class OnRepositoryClickListener implements OnItemClickListener {

        /** The target. */
        private WeakReference<SearchActivity> mTarget;

        /**
         * Instantiates a new on repository click listener.
         *
         * @param activity the activity
         */
        public OnRepositoryClickListener(SearchActivity activity) {
            mTarget = new WeakReference<SearchActivity>(activity);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.widget.AdapterView.OnItemClickListener#onItemClick(android
         * .widget.AdapterView, android.view.View, int, long)
         */
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            if (mTarget.get() != null) {
                SearchRepository repository = (SearchRepository) adapterView.getAdapter().getItem(position);
                mTarget.get().getApplicationContext().openRepositoryInfoActivity(mTarget.get(),
                        repository.getOwner(), repository.getName());
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu,
     * android.view.View, android.view.ContextMenu.ContextMenuInfo)
     */
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
                Repository repository = (Repository) repositoryAdapter.getItem(info.position);
                menu.add("User " + repository.getOwner());
                menu.add("Repo " + repository.getName());
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
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
            if (object instanceof Repository) {
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
            Intent intent = new Intent().setClass(SearchActivity.this, RepositoryActivity.class);
            Repository repository = (Repository) object;
            Bundle data = getApplicationContext().populateRepository(repository);
            intent.putExtra(Constants.DATA_BUNDLE, data);
            startActivity(intent);
        }
        return true;
    }
}