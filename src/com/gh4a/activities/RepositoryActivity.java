package com.gh4a.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v4.util.Pair;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gh4a.BackgroundTask;
import com.gh4a.BasePagerActivity;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.fragment.ContentListFragment;
import com.gh4a.fragment.ContentListFragment.ParentCallback;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.loader.BaseLoader;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.GitModuleParserLoader;
import com.gh4a.loader.IsStarringLoader;
import com.gh4a.loader.IsWatchingLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.loader.TagListLoader;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;

public class RepositoryActivity extends BasePagerActivity implements ParentCallback {
    private static final int LOADER_REPO = 0;
    private static final int LOADER_BRANCHES_AND_TAGS = 1;
    private static final int LOADER_WATCHING = 2;
    private static final int LOADER_STARRING = 3;
    private static final int LOADER_MODULEMAP = 4;

    public static final String EXTRA_INITIAL_PAGE = "initial_page";
    public static final int PAGE_REPO_OVERVIEW = 0;
    public static final int PAGE_FILES = 1;
    public static final int PAGE_COMMITS = 2;

    private static final int[] TITLES = new int[] {
        R.string.about, R.string.repo_files, R.string.commits
    };

    private LoaderCallbacks<Repository> mRepoCallback = new LoaderCallbacks<Repository>() {
        @Override
        public Loader<LoaderResult<Repository>> onCreateLoader(int id, Bundle args) {
            return new RepositoryLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<Repository> result) {
            boolean success = !result.handleError(RepositoryActivity.this);
            if (success) {
                mRepository = result.getData();
                updateTitle();
                setTabsEnabled(true);
                // Apply initial page selection first time the repo is loaded
                if (mInitialPage >= PAGE_REPO_OVERVIEW && mInitialPage <= PAGE_COMMITS) {
                    getPager().setCurrentItem(mInitialPage);
                    mInitialPage = -1;
                }
            }
            setContentEmpty(!success);
            setContentShown(true);
            refreshDone();
            supportInvalidateOptionsMenu();
        }
    };

    private LoaderCallbacks<Map<String, String>> mGitModuleCallback =
            new LoaderCallbacks<Map<String, String>>() {
        @Override
        public Loader<LoaderResult<Map<String, String>>> onCreateLoader(int id, Bundle args) {
            return new GitModuleParserLoader(RepositoryActivity.this, mRepoOwner,
                    mRepoName, ".gitmodules", mSelectedRef);
        }
        @Override
        public void onResultReady(LoaderResult<Map<String, String>> result) {
            mGitModuleMap = result.getData();
            if (mContentListFragment != null) {
                mContentListFragment.onSubModuleNamesChanged(getSubModuleNames(mContentListFragment));
            }
        }
    };

    private LoaderCallbacks<Pair<List<RepositoryBranch>, List<RepositoryTag>>> mBranchesAndTagsCallback =
            new LoaderCallbacks<Pair<List<RepositoryBranch>, List<RepositoryTag>>>() {
        @Override
        public Loader<LoaderResult<Pair<List<RepositoryBranch>, List<RepositoryTag>>>> onCreateLoader(
                int id, Bundle args) {
            return new BaseLoader<Pair<List<RepositoryBranch>, List<RepositoryTag>>>(RepositoryActivity.this) {
                @Override
                protected Pair<List<RepositoryBranch>, List<RepositoryTag>> doLoadInBackground() throws Exception {
                    return Pair.create(new BranchListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground(),
                            new TagListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground());
                }
            };
        }
        @Override
        public void onResultReady(LoaderResult<Pair<List<RepositoryBranch>, List<RepositoryTag>>> result) {
            if (!result.handleError(RepositoryActivity.this)) {
                stopProgressDialog(mProgressDialog);
                mBranches = result.getData().first;
                mTags = result.getData().second;
                showRefSelectionDialog();
                getSupportLoaderManager().destroyLoader(LOADER_BRANCHES_AND_TAGS);
            }
        }
    };

    private LoaderCallbacks<Boolean> mWatchCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsWatchingLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!result.handleError(RepositoryActivity.this)) {
                mIsWatching = result.getData();
                supportInvalidateOptionsMenu();
            }
        }
    };

    private LoaderCallbacks<Boolean> mStarCallback = new LoaderCallbacks<Boolean>() {
        @Override
        public Loader<LoaderResult<Boolean>> onCreateLoader(int id, Bundle args) {
            return new IsStarringLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<Boolean> result) {
            if (!result.handleError(RepositoryActivity.this)) {
                mIsStarring = result.getData();
                supportInvalidateOptionsMenu();
            }
        }
    };

    private String mRepoOwner;
    private String mRepoName;
    private ActionBar mActionBar;
    private ProgressDialog mProgressDialog;
    private int mInitialPage;

    private Stack<String> mDirStack = new Stack<>();
    private Repository mRepository;
    private List<RepositoryBranch> mBranches;
    private List<RepositoryTag> mTags;
    private String mSelectedRef;

    private Boolean mIsWatching;
    private Boolean mIsStarring;

    private RepositoryFragment mRepositoryFragment;
    private ContentListFragment mContentListFragment;
    private CommitListFragment mCommitListFragment;

    private Map<String, String> mGitModuleMap;
    private Map<String, ArrayList<RepositoryContents>> mContentCache =
            new LinkedHashMap<String, ArrayList<RepositoryContents>>() {
        private static final long serialVersionUID = -2379579224736389357L;

        @Override
        protected boolean removeEldestEntry(Entry<String, ArrayList<RepositoryContents>> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    private static final int MAX_CACHE_ENTRIES = 100;

    private static final String STATE_KEY_DIR_STACK = "dir_stack";
    private static final String STATE_KEY_CONTENT_CACHE_PREFIX = "content_cache_";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        if (savedInstanceState != null) {
            for (String entry : savedInstanceState.getStringArrayList(STATE_KEY_DIR_STACK)) {
                mDirStack.add(entry);
            }

            int prefixLen = STATE_KEY_CONTENT_CACHE_PREFIX.length();
            for (String key : savedInstanceState.keySet()) {
                if (key.startsWith(STATE_KEY_CONTENT_CACHE_PREFIX)) {
                    String cacheKey = key.substring(prefixLen);
                    if (cacheKey.equals("/")) {
                        cacheKey = null;
                    }
                    mContentCache.put(cacheKey,
                            (ArrayList<RepositoryContents>) savedInstanceState.getSerializable(key));
                }
            }
        }

        Bundle bundle = getIntent().getExtras();
        mRepoOwner = bundle.getString(Constants.Repository.OWNER);
        mRepoName = bundle.getString(Constants.Repository.NAME);
        mSelectedRef = bundle.getString(Constants.Repository.SELECTED_REF);
        mInitialPage = bundle.getInt(EXTRA_INITIAL_PAGE, -1);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        setEmptyText(R.string.repo_no_data);
        setContentShown(false);
        setTabsEnabled(false);

        getSupportLoaderManager().initLoader(LOADER_REPO, null, mRepoCallback);
        if (Gh4Application.get().isAuthorized()) {
            getSupportLoaderManager().initLoader(LOADER_WATCHING, null, mWatchCallback);
            getSupportLoaderManager().initLoader(LOADER_STARRING, null, mStarCallback);
        }
    }

    private void updateTitle() {
        mActionBar.setSubtitle(getCurrentRef());
        invalidateFragments();
    }

    private String getCurrentRef() {
        if (!TextUtils.isEmpty(mSelectedRef)) {
            return mSelectedRef;
        }
        return mRepository.getMasterBranch();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_KEY_DIR_STACK, new ArrayList<>(mDirStack));
        for (Map.Entry<String, ArrayList<RepositoryContents>> entry : mContentCache.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                key = "/";
            }
            outState.putSerializable(STATE_KEY_CONTENT_CACHE_PREFIX + key, entry.getValue());
        }
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0:
                mRepositoryFragment = RepositoryFragment.newInstance(mRepository, mSelectedRef);
                return mRepositoryFragment;
            case 1:
                if (mDirStack.isEmpty()) {
                    mDirStack.push(null);
                }
                String path = mDirStack.peek();
                mContentListFragment = ContentListFragment.newInstance(mRepository, path,
                        mContentCache.get(path), mSelectedRef);
                return mContentListFragment;
            case 2:
                mCommitListFragment = CommitListFragment.newInstance(mRepository, mSelectedRef);
                return mCommitListFragment;
        }
        return null;
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment fragment) {
        if (fragment instanceof ContentListFragment) {
            if (mContentListFragment == null) {
                return true;
            }
            ContentListFragment clf = (ContentListFragment) fragment;
            if (mDirStack.isEmpty() || !TextUtils.equals(mDirStack.peek(), clf.getPath())) {
                return true;
            }
        } else if (fragment instanceof CommitListFragment && mCommitListFragment == null) {
            return true;
        } else if (fragment instanceof RepositoryFragment && mRepositoryFragment == null) {
            return true;
        }
        return false;
    }

    @Override
    protected boolean canSwipeToRefresh() {
        return true;
    }

    @Override
    public void onRefresh() {
        refreshFragments();
    }

    @Override
    public void onContentsLoaded(ContentListFragment fragment, List<RepositoryContents> contents) {
        if (contents == null) {
            return;
        }
        mContentCache.put(fragment.getPath(), new ArrayList<>(contents));
        if (fragment.getPath() == null) {
            for (RepositoryContents content : contents) {
                if (RepositoryContents.TYPE_FILE.equals(content.getType())) {
                    if (content.getName().equals(".gitmodules")) {
                        LoaderManager lm = getSupportLoaderManager();
                        lm.restartLoader(LOADER_MODULEMAP, null, mGitModuleCallback);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void onTreeSelected(RepositoryContents content) {
        String path = content.getPath();
        if (RepositoryContents.TYPE_DIR.equals(content.getType())) {
            mDirStack.push(path);
            invalidateFragments();
        } else if (mGitModuleMap != null && mGitModuleMap.get(path) != null) {
            String[] userRepo = mGitModuleMap.get(path).split("/");
            startActivity(IntentUtils.getRepoActivityIntent(this, userRepo[0], userRepo[1], null));
        } else {
            startActivity(IntentUtils.getFileViewerActivityIntent(this, mRepoOwner, mRepoName,
                    getCurrentRef(), content.getPath()));
        }
    }

    @Override
    public Set<String> getSubModuleNames(ContentListFragment fragment) {
        if (mGitModuleMap != null && fragment.getPath() == null) {
            return mGitModuleMap.keySet();
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (mDirStack.size() > 1 && getPager().getCurrentItem() == 1) {
            mDirStack.pop();
            invalidateFragments();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.repo_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean authorized = Gh4Application.get().isAuthorized();
        MenuItem watchAction = menu.findItem(R.id.watch);
        watchAction.setVisible(authorized);
        if (authorized) {
            if (mIsWatching == null) {
                MenuItemCompat.setActionView(watchAction, R.layout.ab_loading);
                MenuItemCompat.expandActionView(watchAction);
            } else if (mIsWatching) {
                watchAction.setTitle(R.string.repo_unwatch_action);
            } else {
                watchAction.setTitle(R.string.repo_watch_action);
            }
        }

        MenuItem starAction = menu.findItem(R.id.star);
        starAction.setVisible(authorized);
        if (authorized) {
            if (mIsStarring == null) {
                MenuItemCompat.setActionView(starAction, R.layout.ab_loading);
                MenuItemCompat.expandActionView(starAction);
            } else if (mIsStarring) {
                starAction.setTitle(R.string.repo_unstar_action);
                starAction.setIcon(R.drawable.unstar);
            } else {
                starAction.setTitle(R.string.repo_star_action);
                starAction.setIcon(R.drawable.star);
            }
        }
        if (mRepository == null) {
            menu.removeItem(R.id.ref);
            menu.removeItem(R.id.bookmark);
            menu.removeItem(R.id.zip_download);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getUserActivityIntent(this, mRepoOwner);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.watch:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                AsyncTaskCompat.executeParallel(new UpdateWatchTask());
                return true;
            case R.id.star:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                AsyncTaskCompat.executeParallel(new UpdateStarTask());
                return true;
            case R.id.ref:
                if (mBranches == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().initLoader(LOADER_BRANCHES_AND_TAGS,
                            null, mBranchesAndTagsCallback);
                } else {
                    showRefSelectionDialog();
                }
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, mRepoOwner + "/" + mRepoName);
                shareIntent.putExtra(Intent.EXTRA_TEXT,  "https://github.com/" + mRepoOwner + "/" + mRepoName);
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.bookmark:
                Intent bookmarkIntent = new Intent(this, getClass());
                bookmarkIntent.putExtra(Constants.Repository.OWNER, mRepoOwner);
                bookmarkIntent.putExtra(Constants.Repository.NAME, mRepoName);
                bookmarkIntent.putExtra(Constants.Repository.SELECTED_REF, mSelectedRef);
                saveBookmark(mActionBar.getTitle().toString(), BookmarksProvider.Columns.TYPE_REPO,
                        bookmarkIntent, mActionBar.getSubtitle().toString());
                return true;
            case R.id.zip_download:
                String zipUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName
                        + "/archive/" + getCurrentRef() + ".zip";
                UiUtils.enqueueDownload(this, zipUrl, "application/zip",
                        mRepoName + "-" + getCurrentRef() + ".zip", null, null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showRefSelectionDialog() {
        final BranchAndTagAdapter adapter = new BranchAndTagAdapter();
        int current = -1, master = -1, count = adapter.getCount();

        for (int i = 0; i < count; i++) {
            String name = adapter.getName(i);
            if (name.equals(mSelectedRef) || adapter.getSha(i).equals(mSelectedRef)) {
                current = i;
            }
            if (name.equals(mRepository.getMasterBranch())) {
                master = i;
            }
        }
        if (mSelectedRef == null && current == -1) {
            current = master;
        }

        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setTitle(R.string.repo_select_ref_dialog_title)
                .setSingleChoiceItems(adapter, current, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setSelectedRef(adapter.getName(which));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setSelectedRef(String selectedRef) {
        mSelectedRef = selectedRef;
        clearRefDependentFragments();
        updateTitle();
    }

    private void clearRefDependentFragments() {
        if (mRepositoryFragment != null) {
            mRepositoryFragment.setRef(mSelectedRef);
        }
        mContentListFragment = null;
        mCommitListFragment = null;
        mGitModuleMap = null;
        mDirStack.clear();
        mContentCache.clear();
    }

    private void refreshFragments() {
        mRepositoryFragment = null;
        clearRefDependentFragments();
        setContentShown(false);
        getSupportLoaderManager().getLoader(LOADER_REPO).onContentChanged();
    }

    private class BranchAndTagAdapter extends BaseAdapter {
        private ArrayList<Object> mItems;
        private LayoutInflater mInflater;
        private int mBranchDrawableResId;
        private int mTagDrawableResId;

        public BranchAndTagAdapter() {
            mItems = new ArrayList<>();
            mItems.addAll(mBranches);
            mItems.addAll(mTags);
            mInflater = LayoutInflater.from(RepositoryActivity.this);
            mBranchDrawableResId = UiUtils.resolveDrawable(RepositoryActivity.this, R.attr.branchIcon);
            mTagDrawableResId = UiUtils.resolveDrawable(RepositoryActivity.this, R.attr.tagIcon);
        }

        private String getName(int position) {
            Object item = mItems.get(position);
            if (item instanceof RepositoryBranch) {
                return ((RepositoryBranch) item).getName();
            } else {
                return ((RepositoryTag) item).getName();
            }
        }

        private String getSha(int position) {
            Object item = mItems.get(position);
            if (item instanceof RepositoryBranch) {
                return ((RepositoryBranch) item).getCommit().getSha();
            } else {
                return ((RepositoryTag) item).getCommit().getSha();
            }
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return getName(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.row_branch, parent, false);
            }
            ImageView icon = (ImageView) convertView.findViewById(R.id.icon);
            TextView title = (TextView) convertView.findViewById(R.id.title);

            icon.setImageResource(mItems.get(position) instanceof RepositoryTag
                    ? mTagDrawableResId : mBranchDrawableResId);
            title.setText(getName(position));

            return convertView;
        }
    }

    private class UpdateStarTask extends BackgroundTask<Void> {
        public UpdateStarTask() {
            super(RepositoryActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            StarService starService = (StarService)
                    Gh4Application.get().getService(Gh4Application.STAR_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            if (mIsStarring) {
                starService.unstar(repoId);
            } else {
                starService.star(repoId);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mIsStarring = !mIsStarring;
            if (mRepositoryFragment != null) {
                mRepositoryFragment.updateStargazerCount(mIsStarring);
            }
            supportInvalidateOptionsMenu();
        }
    }

    private class UpdateWatchTask extends BackgroundTask<Void> {
        public UpdateWatchTask() {
            super(RepositoryActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get().getService(Gh4Application.WATCHER_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            if (mIsWatching) {
                watcherService.unwatch(repoId);
            } else {
                watcherService.watch(repoId);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mIsWatching = !mIsWatching;
            supportInvalidateOptionsMenu();
        }
    }
}
