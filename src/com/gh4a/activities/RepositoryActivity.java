package com.gh4a.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
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
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.fragment.ContentListContainerFragment;
import com.gh4a.fragment.RepositoryEventListFragment;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.loader.BaseLoader;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.IsStarringLoader;
import com.gh4a.loader.IsWatchingLoader;
import com.gh4a.loader.LoaderCallbacks;
import com.gh4a.loader.LoaderResult;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.loader.TagListLoader;
import com.gh4a.utils.UiUtils;

import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.RepositoryTag;
import org.eclipse.egit.github.core.service.StarService;
import org.eclipse.egit.github.core.service.WatcherService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RepositoryActivity extends BasePagerActivity {
    public static Intent makeIntent(Context context, Repository repo) {
        return makeIntent(context, repo.getOwner().getLogin(), repo.getName());
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return makeIntent(context, repoOwner, repoName, null);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName, String ref) {
        return makeIntent(context, repoOwner, repoName, ref, null, PAGE_REPO_OVERVIEW);
    }

    public static Intent makeIntent(Context context, String repoOwner, String repoName, String ref,
            String initialPath, int initialPage) {
        if (TextUtils.isEmpty(ref)) {
            ref = null;
        }
        return new Intent(context, RepositoryActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("ref", ref)
                .putExtra("initial_path", initialPath)
                .putExtra("initial_page", initialPage);
    }

    private static final int LOADER_REPO = 0;
    private static final int LOADER_BRANCHES_AND_TAGS = 1;
    private static final int LOADER_WATCHING = 2;
    private static final int LOADER_STARRING = 3;

    public static final int PAGE_REPO_OVERVIEW = 0;
    public static final int PAGE_FILES = 1;
    public static final int PAGE_COMMITS = 2;
    public static final int PAGE_ACTIVITY = 3;

    private static final int[] TITLES = new int[] {
        R.string.about, R.string.repo_files, R.string.commits, R.string.repo_activity
    };

    private final LoaderCallbacks<Repository> mRepoCallback = new LoaderCallbacks<Repository>(this) {
        @Override
        protected Loader<LoaderResult<Repository>> onCreateLoader() {
            return new RepositoryLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }

        @Override
        protected void onResultReady(Repository result) {
            mRepository = result;
            updateTitle();
            invalidateTabs();
            // Apply initial page selection first time the repo is loaded
            if (mInitialPage >= PAGE_REPO_OVERVIEW && mInitialPage <= PAGE_ACTIVITY) {
                getPager().setCurrentItem(mInitialPage);
                mInitialPage = -1;
            }
            setContentShown(true);
            supportInvalidateOptionsMenu();
        }
    };

    private final LoaderCallbacks<Pair<List<RepositoryBranch>, List<RepositoryTag>>> mBranchesAndTagsCallback =
            new LoaderCallbacks<Pair<List<RepositoryBranch>, List<RepositoryTag>>>(this) {
        @Override
        protected Loader<LoaderResult<Pair<List<RepositoryBranch>, List<RepositoryTag>>>> onCreateLoader() {
            return new BaseLoader<Pair<List<RepositoryBranch>, List<RepositoryTag>>>(RepositoryActivity.this) {
                @Override
                protected Pair<List<RepositoryBranch>, List<RepositoryTag>> doLoadInBackground() throws Exception {
                    return Pair.create(new BranchListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground(),
                            new TagListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground());
                }
            };
        }
        @Override
        protected void onResultReady(Pair<List<RepositoryBranch>, List<RepositoryTag>> result) {
            stopProgressDialog(mProgressDialog);
            mBranches = result.first;
            mTags = result.second;
            showRefSelectionDialog();
            getSupportLoaderManager().destroyLoader(LOADER_BRANCHES_AND_TAGS);
        }
        @Override
        protected boolean onError(Exception e) {
            stopProgressDialog(mProgressDialog);
            return false;
        }
    };

    private final LoaderCallbacks<Boolean> mWatchCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsWatchingLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsWatching = result;
            supportInvalidateOptionsMenu();
        }
    };

    private final LoaderCallbacks<Boolean> mStarCallback = new LoaderCallbacks<Boolean>(this) {
        @Override
        protected Loader<LoaderResult<Boolean>> onCreateLoader() {
            return new IsStarringLoader(RepositoryActivity.this, mRepoOwner, mRepoName);
        }
        @Override
        protected void onResultReady(Boolean result) {
            mIsStarring = result;
            supportInvalidateOptionsMenu();
        }
    };

    private String mRepoOwner;
    private String mRepoName;
    private ActionBar mActionBar;
    private ProgressDialog mProgressDialog;
    private int mInitialPage;
    private String mInitialPath;

    private Repository mRepository;
    private List<RepositoryBranch> mBranches;
    private List<RepositoryTag> mTags;
    private String mSelectedRef;

    private Boolean mIsWatching;
    private Boolean mIsStarring;

    private RepositoryFragment mRepositoryFragment;
    private ContentListContainerFragment mContentListFragment;
    private CommitListFragment mCommitListFragment;
    private RepositoryEventListFragment mActivityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        setEmptyText(R.string.repo_no_data);
        setContentShown(false);

        getSupportLoaderManager().initLoader(LOADER_REPO, null, mRepoCallback);
        if (Gh4Application.get().isAuthorized()) {
            getSupportLoaderManager().initLoader(LOADER_WATCHING, null, mWatchCallback);
            getSupportLoaderManager().initLoader(LOADER_STARRING, null, mStarCallback);
        }
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mSelectedRef = extras.getString("ref");
        mInitialPage = extras.getInt("initial_page", -1);
        mInitialPath = extras.getString("initial_path");
    }

    private void updateTitle() {
        mActionBar.setSubtitle(getCurrentRef());
        invalidateFragments();
    }

    private String getCurrentRef() {
        if (!TextUtils.isEmpty(mSelectedRef)) {
            return mSelectedRef;
        }
        return mRepository.getDefaultBranch();
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mRepository != null ? TITLES : null;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0:
                mRepositoryFragment = RepositoryFragment.newInstance(mRepository, mSelectedRef);
                return mRepositoryFragment;
            case 1:
                mContentListFragment = ContentListContainerFragment.newInstance(mRepository,
                        mSelectedRef, mInitialPath);
                mInitialPath = null;
                return mContentListFragment;
            case 2:
                mCommitListFragment = CommitListFragment.newInstance(mRepository, mSelectedRef);
                return mCommitListFragment;
            case 3:
                mActivityFragment = RepositoryEventListFragment.newInstance(mRepository);
                return mActivityFragment;
        }
        return null;
    }

    @Override
    protected boolean fragmentNeedsRefresh(Fragment fragment) {
        if (fragment instanceof CommitListFragment && mCommitListFragment == null) {
            return true;
        } else if (fragment instanceof ContentListContainerFragment
                && mContentListFragment == null) {
            return true;
        } else if (fragment instanceof RepositoryFragment && mRepositoryFragment == null) {
            return true;
        } else if (fragment instanceof RepositoryEventListFragment && mActivityFragment == null) {
            return true;
        }
        return false;
    }

    @Override
    public void onRefresh() {
        mRepositoryFragment = null;
        mContentListFragment = null;
        mActivityFragment = null;
        mRepository = null;
        mIsStarring = null;
        mIsWatching = null;
        mBranches = null;
        mTags = null;
        clearRefDependentFragments();
        setContentShown(false);
        invalidateTabs();
        forceLoaderReload(0, 1, 2, 3);
        super.onRefresh();
    }

    @Override
    public void onBackPressed() {
        if (mContentListFragment != null) {
            if (getPager().getCurrentItem() == 1 && mContentListFragment.handleBackPress()) {
                return;
            }
        }
        super.onBackPressed();
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
        return UserActivity.makeIntent(this, mRepoOwner);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.watch:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                new UpdateWatchTask().schedule();
                return true;
            case R.id.star:
                MenuItemCompat.setActionView(item, R.layout.ab_loading);
                MenuItemCompat.expandActionView(item);
                new UpdateStarTask().schedule();
                return true;
            case R.id.ref:
                if (mBranches == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg));
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
                Intent bookmarkIntent = makeIntent(this, mRepoOwner, mRepoName, mSelectedRef);
                saveBookmark(mActionBar.getTitle().toString(), BookmarksProvider.Columns.TYPE_REPO,
                        bookmarkIntent, mActionBar.getSubtitle().toString());
                return true;
            case R.id.zip_download:
                String zipUrl = "https://github.com/" + mRepoOwner + "/" + mRepoName
                        + "/archive/" + getCurrentRef() + ".zip";
                UiUtils.enqueueDownloadWithPermissionCheck(this, zipUrl, "application/zip",
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
            if (name.equals(mRepository.getDefaultBranch())) {
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
        if (mContentListFragment != null) {
            mContentListFragment.setRef(mSelectedRef);
        }
        mCommitListFragment = null;
    }

    private class BranchAndTagAdapter extends BaseAdapter {
        private final ArrayList<Object> mItems;
        private final LayoutInflater mInflater;
        private final int mBranchDrawableResId;
        private final int mTagDrawableResId;

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
            if (mIsStarring == null) {
                // user refreshed while the action was in progress
                return;
            }
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
            if (mIsWatching == null) {
                // user refreshed while the action was in progress
                return;
            }
            mIsWatching = !mIsWatching;
            supportInvalidateOptionsMenu();
        }
    }
}
