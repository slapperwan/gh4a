package com.gh4a.fragment;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.gh4a.ApiRequestException;
import com.gh4a.BackgroundTask;
import com.gh4a.R;
import com.gh4a.activities.FileViewerActivity;
import com.gh4a.activities.RepositoryActivity;
import com.gh4a.activities.UserActivity;
import com.gh4a.adapter.CodeSearchAdapter;
import com.gh4a.adapter.RepositoryAdapter;
import com.gh4a.adapter.RootAdapter;
import com.gh4a.adapter.SearchUserAdapter;
import com.gh4a.db.SuggestionsProvider;
import com.gh4a.loader.CodeSearchLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositorySearchLoader;
import com.gh4a.loader.UserSearchLoader;
import com.gh4a.utils.StringUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.ClientErrorResponse;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.SearchCode;
import com.meisolsson.githubsdk.model.TextMatch;
import com.meisolsson.githubsdk.model.User;

import java.util.List;

public class SearchFragment extends LoadingListFragmentBase implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener,
        SearchView.OnSuggestionListener, FilterQueryProvider,
        AdapterView.OnItemSelectedListener, RootAdapter.OnItemClickListener,
        CodeSearchAdapter.Callback {
    public static SearchFragment newInstance(int initialType, String initialQuery) {
        SearchFragment f = new SearchFragment();
        Bundle args = new Bundle();
        args.putInt("search_type", initialType);
        args.putString("initial_search", initialQuery);
        f.setArguments(args);
        return f;
    }

    private static final int SEARCH_TYPE_NONE = -1;
    public static final int SEARCH_TYPE_REPO = 0;
    public static final int SEARCH_TYPE_USER = 1;
    public static final int SEARCH_TYPE_CODE = 2;

    private static final int[][] HINT_AND_EMPTY_TEXTS = {
        { R.string.search_hint_repo, R.string.no_search_repos_found },
        { R.string.search_hint_user, R.string.no_search_users_found },
        { R.string.search_hint_code, R.string.no_search_code_found }
    };

    private static final String[] SUGGESTION_PROJECTION = {
            SuggestionsProvider.Columns._ID, SuggestionsProvider.Columns.SUGGESTION
    };
    private static final String SUGGESTION_SELECTION =
            SuggestionsProvider.Columns.TYPE + " = ? AND " +
                    SuggestionsProvider.Columns.SUGGESTION + " LIKE ?";
    private static final String SUGGESTION_ORDER = SuggestionsProvider.Columns.DATE + " DESC";

    private static final String STATE_KEY_QUERY = "query";
    private static final String STATE_KEY_SEARCH_TYPE = "search_type";

    private final LoaderCallbacks<List<Repository>> mRepoCallback =
            new LoaderCallbacks<List<Repository>>(this) {
        @Override
        protected Loader<LoaderResult<List<Repository>>> onCreateLoader() {
            RepositorySearchLoader loader = new RepositorySearchLoader(getActivity(), null);
            loader.setQuery(mQuery);
            return loader;
        }

        @Override
        protected void onResultReady(List<Repository> result) {
            RepositoryAdapter adapter = new RepositoryAdapter(getActivity());
            adapter.addAll(result);
            setAdapter(adapter);
        }
    };

    private final LoaderCallbacks<List<User>> mUserCallback =
            new LoaderCallbacks<List<User>>(this) {
        @Override
        protected Loader<LoaderResult<List<User>>> onCreateLoader() {
            return new UserSearchLoader(getActivity(), mQuery);
        }

        @Override
        protected void onResultReady(List<User> result) {
            SearchUserAdapter adapter = new SearchUserAdapter(getActivity());
            adapter.addAll(result);
            setAdapter(adapter);
        }
    };

    private final LoaderCallbacks<List<SearchCode>> mCodeCallback =
            new LoaderCallbacks<List<SearchCode>>(this) {
        @Override
        protected Loader<LoaderResult<List<SearchCode>>> onCreateLoader() {
            return new CodeSearchLoader(getActivity(), mQuery);
        }

        @Override
        protected void onResultReady(List<SearchCode> result) {
            CodeSearchAdapter adapter = new CodeSearchAdapter(getActivity(), SearchFragment.this);
            adapter.addAll(result);
            setAdapter(adapter);
        }

        @Override
        protected boolean onError(Exception e) {
            if (e instanceof ApiRequestException) {
                ClientErrorResponse response = ((ApiRequestException) e).getResponse();
                if (response!= null && response.errors() != null && !response.errors().isEmpty()) {
                    updateEmptyText(R.string.code_search_too_broad);
                    if (mAdapter != null) {
                        mAdapter.clear();
                    }
                    updateEmptyState();
                    setContentShown(true);
                    return true;
                }
            }
            return super.onError(e);
        }
    };

    private RecyclerView mRecyclerView;
    private RootAdapter<?, ?> mAdapter;

    private Spinner mSearchType;
    private SearchView mSearch;
    private int mInitialSearchType;
    private String mQuery;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if (savedInstanceState != null) {
            mQuery = savedInstanceState.getString(STATE_KEY_QUERY);
            mInitialSearchType = savedInstanceState.getInt(STATE_KEY_SEARCH_TYPE, SEARCH_TYPE_NONE);

            LoaderManager lm = getLoaderManager();
            switch (mInitialSearchType) {
                case SEARCH_TYPE_REPO: lm.initLoader(0, null, mRepoCallback); break;
                case SEARCH_TYPE_USER: lm.initLoader(0, null, mUserCallback); break;
                case SEARCH_TYPE_CODE: lm.initLoader(0, null, mCodeCallback); break;
            }
        } else {
            Bundle args = getArguments();
            mInitialSearchType = args.getInt("search_type", SEARCH_TYPE_REPO);
            mQuery = args.getString("initial_search");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.search, menu);

        mSearchType = (Spinner) menu.findItem(R.id.type).getActionView();
        mSearchType.setAdapter(new SearchTypeAdapter(mSearchType.getContext(), getActivity()));
        mSearchType.setOnItemSelectedListener(this);
        if (mInitialSearchType != SEARCH_TYPE_NONE) {
            mSearchType.setSelection(mInitialSearchType);
            mInitialSearchType = SEARCH_TYPE_NONE;
        }

        SuggestionAdapter adapter = new SuggestionAdapter(getActivity());
        adapter.setFilterQueryProvider(this);

        mSearch = (SearchView) menu.findItem(R.id.search).getActionView();
        mSearch.setIconifiedByDefault(true);
        mSearch.requestFocus();
        mSearch.onActionViewExpanded();
        mSearch.setIconified(false);
        mSearch.setOnQueryTextListener(this);
        mSearch.setOnCloseListener(this);
        mSearch.setOnSuggestionListener(this);
        mSearch.setSuggestionsAdapter(adapter);
        if (mQuery != null) {
            mSearch.setQuery(mQuery, false);
        }

        updateSelectedSearchType();

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mAdapter instanceof RepositoryAdapter) {
            outState.putInt(STATE_KEY_SEARCH_TYPE, SEARCH_TYPE_REPO);
        } else if (mAdapter instanceof SearchUserAdapter) {
            outState.putInt(STATE_KEY_SEARCH_TYPE, SEARCH_TYPE_USER);
        } else if (mAdapter instanceof CodeSearchAdapter) {
            outState.putInt(STATE_KEY_SEARCH_TYPE, SEARCH_TYPE_CODE);
        }
        outState.putString(STATE_KEY_QUERY, mQuery);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRefresh() {
        if (mAdapter != null) {
            hideContentAndRestartLoaders(0);
        }
    }

    @Override
    protected int getEmptyTextResId() {
        return 0; // will be updated later
    }

    @Override
    protected void onRecyclerViewInflated(RecyclerView view, LayoutInflater inflater) {
        super.onRecyclerViewInflated(view, inflater);
        mRecyclerView = view;
    }

    @Override
    public void onItemClick(Object item) {
        if (item instanceof Repository) {
            Repository repository = (Repository) item;
            startActivity(RepositoryActivity.makeIntent(getActivity(), repository));
        } else if (item instanceof SearchCode) {
            openFileViewer((SearchCode) item, -1);
        } else {
            User user = (User) item;
            startActivity(UserActivity.makeIntent(getActivity(), user));
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mQuery = query;
        if (!StringUtils.isBlank(query)) {
            int type = mSearchType.getSelectedItemPosition();
            new SaveSearchSuggestionTask(query, type).schedule();
        }
        loadResults();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mQuery = newText;
        return true;
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
    public boolean onSuggestionSelect(int position) {
        return false;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        Cursor cursor = mSearch.getSuggestionsAdapter().getCursor();
        if (cursor.moveToPosition(position)) {
            if (position == cursor.getCount() - 1) {
                new SuggestionDeletionTask(mSearchType.getSelectedItemPosition()).schedule();
            } else {
                mQuery = cursor.getString(1);
                mSearch.setQuery(mQuery, false);
            }
        }
        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        updateSelectedSearchType();
        if (getLoaderManager().getLoader(0) != null) {
            loadResults();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        updateSelectedSearchType();
    }

    @Override
    public void onSearchFragmentClick(SearchCode result, int matchIndex) {
        openFileViewer(result, matchIndex);
    }

    @Override
    public Cursor runQuery(CharSequence query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        int type = mSearchType.getSelectedItemPosition();
        return getContext().getContentResolver().query(SuggestionsProvider.Columns.CONTENT_URI,
                SUGGESTION_PROJECTION, SUGGESTION_SELECTION,
                new String[] { String.valueOf(type), query + "%" }, SUGGESTION_ORDER);
    }

    private void openFileViewer(SearchCode result, int matchIndex) {
        Repository repo = result.repository();
        Uri uri = Uri.parse(result.url());
        String ref = uri.getQueryParameter("ref");
        TextMatch textMatch = matchIndex >= 0 ? result.textMatches().get(matchIndex) : null;
        startActivity(FileViewerActivity.makeIntentWithSearchMatch(getActivity(),
                repo.owner().login(), repo.name(), ref, result.path(),
                textMatch));
    }

    private void loadResults() {
        LoaderManager lm = getLoaderManager();
        switch (mSearchType.getSelectedItemPosition()) {
            case SEARCH_TYPE_USER: lm.restartLoader(0, null, mUserCallback); break;
            case SEARCH_TYPE_CODE: lm.restartLoader(0, null, mCodeCallback); break;
            default: lm.restartLoader(0, null, mRepoCallback); break;
        }
        setContentShown(false);
        mSearch.clearFocus();
    }

    private void updateSelectedSearchType() {
        int[] hintAndEmptyTextResIds = HINT_AND_EMPTY_TEXTS[mSearchType.getSelectedItemPosition()];
        mSearch.setQueryHint(getString(hintAndEmptyTextResIds[0]));
        updateEmptyText(hintAndEmptyTextResIds[1]);

        // force re-filtering of the view
        mSearch.setQuery(mQuery, false);
    }

    private void setAdapter(RootAdapter<?, ?> adapter) {
        adapter.setOnItemClickListener(this);
        mRecyclerView.setAdapter(adapter);
        mAdapter = adapter;
        setContentShown(true);
        updateEmptyState();
    }

    private void updateEmptyText(@StringRes int emptyTextResId) {
        TextView emptyView = getView().findViewById(android.R.id.empty);
        emptyView.setText(emptyTextResId);
    }

    private class SaveSearchSuggestionTask extends BackgroundTask<Void> {
        private final String mSuggestion;
        private final int mType;

        public SaveSearchSuggestionTask(String suggestion, int type) {
            super(getBaseActivity());
            mSuggestion = suggestion;
            mType = type;
        }

        @Override
        protected Void run() throws ApiRequestException {
            ContentValues cv = new ContentValues();
            cv.put(SuggestionsProvider.Columns.TYPE, mType);
            cv.put(SuggestionsProvider.Columns.SUGGESTION, mSuggestion);
            cv.put(SuggestionsProvider.Columns.DATE, System.currentTimeMillis());
            getContext().getContentResolver().insert(SuggestionsProvider.Columns.CONTENT_URI, cv);
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
        }
    }

    private class SuggestionDeletionTask extends BackgroundTask<Void> {
        private final int mType;

        public SuggestionDeletionTask(int type) {
            super(getBaseActivity());
            mType = type;
        }

        @Override
        protected Void run() {
            getContext().getContentResolver().delete(SuggestionsProvider.Columns.CONTENT_URI,
                    SuggestionsProvider.Columns.TYPE + " = ?",
                    new String[] { String.valueOf(mType) });
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
        }
    }

    private static class SearchTypeAdapter extends BaseAdapter implements SpinnerAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private final LayoutInflater mPopupInflater;

        private final int[][] mResources = new int[][] {
            { R.string.search_type_repo, R.drawable.icon_repositories_dark, R.attr.searchRepoIcon, 0 },
            { R.string.search_type_user, R.drawable.search_users_dark, R.attr.searchUserIcon, 0 },
            { R.string.search_type_code, R.drawable.search_code_dark, R.attr.searchCodeIcon, 0 }
        };

        private SearchTypeAdapter(Context context, Context popupContext) {
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

            ImageView icon = convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][1]);

            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mPopupInflater.inflate(R.layout.search_type_popup, null);
            }

            ImageView icon = convertView.findViewById(R.id.icon);
            icon.setImageResource(mResources[position][3]);

            TextView label = convertView.findViewById(R.id.label);
            label.setText(mResources[position][0]);

            return convertView;
        }
    }


    private static class SuggestionAdapter extends CursorAdapter {
        private final LayoutInflater mInflater;

        public SuggestionAdapter(Context context) {
            super(context, null, false);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor != null && newCursor.getCount() > 0) {
                MatrixCursor clearRowCursor = new MatrixCursor(SUGGESTION_PROJECTION);
                clearRowCursor.addRow(new Object[] {
                        Long.MAX_VALUE,
                        mContext.getString(R.string.clear_suggestions)
                });
                newCursor = new MergeCursor(new Cursor[] { newCursor, clearRowCursor });
            }
            return super.swapCursor(newCursor);
        }

        @Override
        public int getItemViewType(int position) {
            return isClearRow(position) ? 1 : 0;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            @LayoutRes int layoutResId = isClearRow(cursor.getPosition())
                    ? R.layout.row_suggestion_clear : R.layout.row_suggestion;
            return mInflater.inflate(layoutResId, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView textView = (TextView) view;
            int columnIndex = cursor.getColumnIndexOrThrow(SuggestionsProvider.Columns.SUGGESTION);
            textView.setText(cursor.getString(columnIndex));
        }

        private boolean isClearRow(int position) {
            return position == getCount() - 1;
        }
    }
}
