package com.gh4a.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.view.ViewGroup;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.BackgroundTask;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.fragment.ContentListFragment;
import com.gh4a.fragment.ContentListFragment.ParentCallback;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.GitModuleParserLoader;
import com.gh4a.loader.IsStarringLoader;
import com.gh4a.loader.IsWatchingLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.loader.TagListLoader;
import com.gh4a.utils.StringUtils;

public class RepositoryActivity extends BaseSherlockFragmentActivity implements ParentCallback {
    private static final int LOADER_REPO = 0;
    private static final int LOADER_BRANCHES = 1;
    private static final int LOADER_TAGS = 2;
    private static final int LOADER_WATCHING = 3;
    private static final int LOADER_STARRING = 4;
    private static final int LOADER_MODULEMAP = 5;

    private LoaderCallbacks<Repository> mRepoCallback = new LoaderCallbacks<Repository>() {
        @Override
        public Loader<LoaderResult<Repository>> onCreateLoader(int id, Bundle args) {
            return new RepositoryLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        public void onResultReady(LoaderResult<Repository> result) {
            if (!checkForError(result)) {
                hideLoading();
                mRepository = result.getData();
                initializePager();
                invalidateOptionsMenu();
            }
        }
    };

    private LoaderCallbacks<Map<String, String>> mGitModuleCallback =
            new LoaderCallbacks<Map<String, String>>() {
        @Override
        public Loader<LoaderResult<Map<String, String>>> onCreateLoader(int id, Bundle args) {
            return new GitModuleParserLoader(RepositoryActivity.this, mRepository.getOwner().getLogin(),
                    mRepository.getName(), ".gitmodules", mSelectedRef);
        }
        @Override
        public void onResultReady(LoaderResult<Map<String, String>> result) {
            mGitModuleMap = result.getData();
        }
    };

    private LoaderCallbacks<List<RepositoryBranch>> mBranchCallback =
            new LoaderCallbacks<List<RepositoryBranch>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryBranch>>> onCreateLoader(int id, Bundle args) {
            return new BranchListLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryBranch>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mBranches = result.getData();
                showBranchesDialog();
            }
        }
    };

    private LoaderCallbacks<List<RepositoryTag>> mTagCallback =
            new LoaderCallbacks<List<RepositoryTag>>() {
        @Override
        public Loader<LoaderResult<List<RepositoryTag>>> onCreateLoader(int id, Bundle args) {
            return new TagListLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        public void onResultReady(LoaderResult<List<RepositoryTag>> result) {
            if (!checkForError(result)) {
                stopProgressDialog(mProgressDialog);
                mTags = (List<RepositoryTag>) result.getData();
                showTagsDialog();
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
            if (!checkForError(result)) {
                mIsWatching = result.getData();
                mIsFinishLoadingWatching = true;
                invalidateOptionsMenu();
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
            if (!checkForError(result)) {
                mIsStarring = result.getData();
                mIsFinishLoadingStarring = true;
                invalidateOptionsMenu();
            }
        }
    };

    private String mRepoOwner;
    private String mRepoName;
    private RepositoryAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private Stack<ContentListFragment> mDirStack;
    private Repository mRepository;
    private List<RepositoryBranch> mBranches;
    private List<RepositoryTag> mTags;
    private String mSelectedRef;
    private String mSelectBranchTag;
    private ProgressDialog mProgressDialog;
    private boolean mIsFinishLoadingWatching;
    private boolean mIsWatching;
    private boolean mIsFinishLoadingStarring;
    private boolean mIsStarring;
    private RepositoryFragment mRepositoryFragment;
    private ContentListFragment mContentListFragment;
    private CommitListFragment mCommitListFragment;
    private Map<String, String> mGitModuleMap;

    private Map<String, ArrayList<RepositoryContents>> mContentCache;
    private static final int MAX_CACHE_ENTRIES = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);

        mDirStack = new Stack<ContentListFragment>();
        mContentCache = new LinkedHashMap<String, ArrayList<RepositoryContents>>() {
            private static final long serialVersionUID = -2379579224736389357L;
            @Override
            protected boolean removeEldestEntry(Entry<String, ArrayList<RepositoryContents>> eldest) {
                return size() > MAX_CACHE_ENTRIES;
            }
        };
        
        Bundle data = getIntent().getExtras().getBundle(Constants.DATA_BUNDLE);
        if (data != null) {
            mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
            mRepoName = data.getString(Constants.Repository.REPO_NAME);
        }
        else {
            Bundle bundle = getIntent().getExtras();
            mRepoOwner = bundle.getString(Constants.Repository.REPO_OWNER);
            mRepoName = bundle.getString(Constants.Repository.REPO_NAME);
            mSelectedRef = bundle.getString(Constants.Repository.SELECTED_REF);
            mSelectBranchTag = bundle.getString(Constants.Repository.SELECTED_BRANCHTAG_NAME);
        }
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.view_pager);
        
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();

        getSupportLoaderManager().initLoader(LOADER_REPO, null, mRepoCallback);
        getSupportLoaderManager().initLoader(LOADER_BRANCHES, null, mBranchCallback);
        getSupportLoaderManager().initLoader(LOADER_TAGS, null, mTagCallback);
        getSupportLoaderManager().initLoader(LOADER_WATCHING, null, mWatchCallback);
        getSupportLoaderManager().initLoader(LOADER_STARRING, null, mStarCallback);

        getSupportLoaderManager().getLoader(LOADER_REPO).forceLoad();
        getSupportLoaderManager().getLoader(LOADER_WATCHING).forceLoad();
        getSupportLoaderManager().getLoader(LOADER_STARRING).forceLoad();

        mAdapter = new RepositoryAdapter(getSupportFragmentManager());
    }

    private void initializePager() {
        if (mPager != null) {
            mAdapter.notifyDataSetChanged();
        } else {
            mPager = setupPager(mAdapter, new int[] {
                    R.string.about, R.string.repo_files, R.string.commits
            });
        }
        mActionBar.setSubtitle(StringUtils.isBlank(mSelectBranchTag) ?
                mRepository.getMasterBranch() : mSelectBranchTag);
    }

    public class RepositoryAdapter extends FragmentStatePagerAdapter {

        public RepositoryContents mContent;
        
        public RepositoryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 1) {
                if (mContentListFragment == null) {
                    mContentListFragment = ContentListFragment.newInstance(mRepository, null,
                            mContentCache.get(null), mSelectedRef);
                    mDirStack.add(mContentListFragment);
                }
                else {
                    mContentListFragment = mDirStack.peek();
                }
                return mContentListFragment;
            }
            
            else if (position == 2) {
                mCommitListFragment = CommitListFragment.newInstance(mRepository, mSelectedRef);
                return mCommitListFragment;
            }

            else {
                mRepositoryFragment = RepositoryFragment.newInstance(mRepository);
                return mRepositoryFragment;
            }
        }
        
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (position == 1) {
                super.destroyItem(container, position, object);
            }
        }
        
        @Override
        public int getItemPosition(Object object) {
            if (object instanceof ContentListFragment) {
                if (mDirStack.isEmpty() || mContentListFragment == null
                        || mContentListFragment != mDirStack.peek()) {
                    return POSITION_NONE;
                }
            }
            return POSITION_UNCHANGED;
        }
    }

    @Override
    public void onModuleMapFound(ContentListFragment fragment) {
        if (!mDirStack.isEmpty() && mDirStack.get(0) == fragment) {
            LoaderManager lm = getSupportLoaderManager();
            if (lm.getLoader(LOADER_MODULEMAP) == null) {
                lm.initLoader(LOADER_MODULEMAP, null, mGitModuleCallback);
            }
            else {
                lm.restartLoader(LOADER_MODULEMAP, null, mGitModuleCallback);
            }
            lm.getLoader(LOADER_MODULEMAP).forceLoad();
        }
    }

    @Override
    public void onTreeSelected(ContentListFragment fragment, RepositoryContents content, String ref) {
        String path = content.getPath();
        if (RepositoryContents.TYPE_DIR.equals(content.getType())) {
            mAdapter.mContent = content;
            mSelectedRef = ref;
            mDirStack.push(ContentListFragment.newInstance(mRepository, path, mContentCache.get(path), mSelectedRef));
            mAdapter.notifyDataSetChanged();
        }
        else if (mGitModuleMap != null && mGitModuleMap.get(path) != null) {
            String[] userRepo = mGitModuleMap.get(path).split("/");
            Gh4Application.get(this).openRepositoryInfoActivity(this, userRepo[0], userRepo[1], 0);
        }
        else {
            openFileViewer(content, ref);
        }
    }
    
    private void openFileViewer(RepositoryContents content, String ref) {
        Intent intent = new Intent().setClass(this, FileViewerActivity.class);
        intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
        intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
        intent.putExtra(Constants.Object.PATH, content.getPath());
        intent.putExtra(Constants.Object.REF, ref);
        intent.putExtra(Constants.Object.NAME, content.getName());
        intent.putExtra(Constants.Object.OBJECT_SHA, content.getSha());
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        if (mPager != null && mPager.getCurrentItem() == 1 && mDirStack.size() > 1) {
            ContentListFragment fragment = mDirStack.pop();
            mContentCache.put(fragment.getPath(),
                    new ArrayList<RepositoryContents>(fragment.getContents()));
            mAdapter.notifyDataSetChanged();
        }
        else {
            super.onBackPressed();
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.repo_menu, menu);
        
        if (Gh4Application.THEME != R.style.LightTheme) {
            menu.findItem(R.id.refresh).setIcon(R.drawable.navigation_refresh_dark);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem watchAction = menu.findItem(R.id.watch);
        watchAction.setVisible(isAuthorized());
        if (isAuthorized()) {
            if (!mIsFinishLoadingWatching) {
                watchAction.setActionView(R.layout.ab_loading);
                watchAction.expandActionView();
            }
            else if (mIsWatching) {
                watchAction.setTitle(R.string.repo_unwatch_action);
            }
            else {
                watchAction.setTitle(R.string.repo_watch_action);
            }
        }
        
        MenuItem starAction = menu.findItem(R.id.star);
        starAction.setVisible(isAuthorized());
        if (isAuthorized()) {
            if (!mIsFinishLoadingStarring) {
                starAction.setActionView(R.layout.ab_loading);
                starAction.expandActionView();
            }
            else if (mIsStarring) {
                starAction.setTitle(R.string.repo_unstar_action);
            }
            else {
                starAction.setTitle(R.string.repo_star_action);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected void navigateUp() {
        Gh4Application.get(this).openUserInfoActivity(this, mRepoOwner, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.watch:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                new UpdateWatchTask().execute();
                return true;
            case R.id.star:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                new UpdateStarTask().execute();
                return true;
            case R.id.branches:
                if (mBranches == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(LOADER_BRANCHES).forceLoad();
                }
                else {
                    showBranchesDialog();
                }
                return true;
            case R.id.tags:
                if (mTags == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(LOADER_TAGS).forceLoad();
                }
                else {
                    showTagsDialog();
                }
                return true;
            case R.id.refresh:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                refreshFragment();
                return true;
            case R.id.share:
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, mRepoOwner + "/" + mRepoName);
                shareIntent.putExtra(Intent.EXTRA_TEXT,  mRepository.getHtmlUrl());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.share_title));
                startActivity(shareIntent);
                return true;
            case R.id.bookmark:
                Intent bookmarkIntent = new Intent(this, getClass());
                bookmarkIntent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
                bookmarkIntent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
                bookmarkIntent.putExtra(Constants.Repository.SELECTED_REF, mSelectedRef);
                bookmarkIntent.putExtra(Constants.Repository.SELECTED_BRANCHTAG_NAME, mSelectBranchTag);
                saveBookmark(mActionBar.getTitle().toString(), BookmarksProvider.Columns.TYPE_REPO,
                        bookmarkIntent, mActionBar.getSubtitle().toString());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void showBranchesDialog() {
        String[] branchList = new String[mBranches.size()];
        for (int i = 0; i < mBranches.size(); i++) {
            branchList[i] = mBranches.get(i).getName();
        }
        
        AlertDialog.Builder builder = createDialogBuilder();
        builder.setCancelable(true);
        builder.setTitle(R.string.repo_branches);
        builder.setSingleChoiceItems(branchList, -1, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedRef = mBranches.get(which).getCommit().getSha();
                mSelectBranchTag = mBranches.get(which).getName();
            }
        });
        
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                refreshFragment();
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        
        builder.show();
    }
    
    private void showTagsDialog() {
        String[] tagList = new String[mTags.size()];
        for (int i = 0; i < mTags.size(); i++) {
            tagList[i] = mTags.get(i).getName();
        }
        
        AlertDialog.Builder builder = createDialogBuilder();
        builder.setCancelable(true);
        builder.setTitle(R.string.repo_tags);
        builder.setSingleChoiceItems(tagList, -1, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedRef = mTags.get(which).getCommit().getSha();
                mSelectBranchTag = mTags.get(which).getName();
            }
        });
        
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                refreshFragment();
            }
        });
        
        builder.show();
    }
    
    private void refreshFragment() {
        mRepositoryFragment = null;
        mContentListFragment = null;
        mCommitListFragment = null;
        mGitModuleMap = null;
        mDirStack.clear();
        mContentCache.clear();
        showLoading();
        getSupportLoaderManager().restartLoader(LOADER_REPO, null, mRepoCallback);
        getSupportLoaderManager().getLoader(LOADER_REPO).forceLoad();
    }

    private boolean checkForError(LoaderResult<?> result) {
        if (isLoaderError(result)) {
            hideLoading();
            stopProgressDialog(mProgressDialog);
            invalidateOptionsMenu();
            return true;
        }
        return false;
    }

    private class UpdateStarTask extends BackgroundTask<Void> {
        public UpdateStarTask() {
            super(RepositoryActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            StarService starService = (StarService)
                    Gh4Application.get(mContext).getService(Gh4Application.STAR_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            if (mIsStarring) {
                starService.unstar(repoId);
            }
            else {
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
            invalidateOptionsMenu();
        }
    }

    private class UpdateWatchTask extends BackgroundTask<Void> {
        public UpdateWatchTask() {
            super(RepositoryActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            WatcherService watcherService = (WatcherService)
                    Gh4Application.get(mContext).getService(Gh4Application.WATCHER_SERVICE);
            RepositoryId repoId = new RepositoryId(mRepoOwner, mRepoName);
            if (mIsWatching) {
                watcherService.unwatch(repoId);
            }
            else {
                watcherService.watch(repoId);
            }
            return null;
        }

        @Override
        protected void onSuccess(Void result) {
            mIsWatching = !mIsWatching;
            invalidateOptionsMenu();
        }
    }
}
