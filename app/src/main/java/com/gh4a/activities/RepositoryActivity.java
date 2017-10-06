package com.gh4a.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
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
import com.gh4a.BaseFragmentPagerActivity;
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
import com.gh4a.loader.ProgressDialogLoaderCallbacks;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.loader.TagListLoader;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Branch;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.StarringService;
import com.meisolsson.githubsdk.service.activity.WatchingService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class RepositoryActivity extends BaseFragmentPagerActivity {
    public static Intent makeIntent(Context context, Repository repo) {
        return makeIntent(context, repo, null);
    }

    public static Intent makeIntent(Context context, Repository repo, String ref) {
        return makeIntent(context, repo.owner().login(), repo.name(), ref);
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

    private final LoaderCallbacks<Pair<List<Branch>, List<Branch>>> mBranchesAndTagsCallback =
            new ProgressDialogLoaderCallbacks<Pair<List<Branch>, List<Branch>>>(this, this) {
        @Override
        protected Loader<LoaderResult<Pair<List<Branch>, List<Branch>>>> onCreateLoader() {
            return new BaseLoader<Pair<List<Branch>, List<Branch>>>(RepositoryActivity.this) {
                @Override
                protected Pair<List<Branch>, List<Branch>> doLoadInBackground() throws Exception {
                    return Pair.create(new BranchListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground(),
                            new TagListLoader(getContext(), mRepoOwner, mRepoName).doLoadInBackground());
                }
            };
        }
        @Override
        protected void onResultReady(Pair<List<Branch>, List<Branch>> result) {
            mBranches = result.first;
            mTags = result.second;
            showRefSelectionDialog();
            getSupportLoaderManager().destroyLoader(LOADER_BRANCHES_AND_TAGS);
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
    private int mInitialPage;
    private String mInitialPath;

    private Repository mRepository;
    private List<Branch> mBranches;
    private List<Branch> mTags;
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
        return mRepository.defaultBranch();
    }

    private String getBookmarkUrl() {
        String url = "https://github.com/" + mRepoOwner + "/" + mRepoName;
        String ref = getCurrentRef();
        return ref.equals(mRepository.defaultBranch()) ? url : url + "/tree/" + ref;
    }

    @Override
    protected int[] getTabTitleResIds() {
        return mRepository != null ? TITLES : null;
    }

    @Override
    protected Fragment makeFragment(int position) {
        switch (position) {
            case 0:
                return RepositoryFragment.newInstance(mRepository, mSelectedRef);
            case 1:
                Fragment f = ContentListContainerFragment.newInstance(mRepository,
                        mSelectedRef, mInitialPath);
                mInitialPath = null;
                return f;
            case 2:
                return CommitListFragment.newInstance(mRepository, mSelectedRef);
            case 3:
                return RepositoryEventListFragment.newInstance(mRepository);
        }
        return null;
    }

    @Override
    protected void onFragmentInstantiated(Fragment f, int position) {
        switch (position) {
            case 0: mRepositoryFragment = (RepositoryFragment) f; break;
            case 1: mContentListFragment = (ContentListContainerFragment) f; break;
            case 2: mCommitListFragment = (CommitListFragment) f; break;
            case 3: mActivityFragment = (RepositoryEventListFragment) f; break;
        }
    }

    @Override
    protected void onFragmentDestroyed(Fragment f) {
        if (f == mRepositoryFragment) {
            mRepositoryFragment = null;
        } else if (f == mContentListFragment) {
            mContentListFragment = null;
        } else if (f == mCommitListFragment) {
            mCommitListFragment = null;
        } else if (f == mActivityFragment) {
            mActivityFragment = null;
        }
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
                watchAction.setActionView(R.layout.ab_loading);
                watchAction.expandActionView();
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
                starAction.setActionView(R.layout.ab_loading);
                starAction.expandActionView();
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
        } else {
            MenuItem bookmarkAction = menu.findItem(R.id.bookmark);
            if (bookmarkAction != null) {
                bookmarkAction.setTitle(BookmarksProvider.hasBookmarked(this, getBookmarkUrl())
                        ? R.string.remove_bookmark
                        : R.string.bookmark);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    protected Intent navigateUp() {
        return UserActivity.makeIntent(this, mRepoOwner);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String url = "https://github.com/" + mRepoOwner + "/" + mRepoName;
        switch (item.getItemId()) {
            case R.id.watch:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                new UpdateWatchTask().schedule();
                return true;
            case R.id.star:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                new UpdateStarTask().schedule();
                return true;
            case R.id.ref:
                if (mBranches == null) {
                    getSupportLoaderManager().initLoader(LOADER_BRANCHES_AND_TAGS,
                            null, mBranchesAndTagsCallback);
                } else {
                    showRefSelectionDialog();
                }
                return true;
            case R.id.share:
                IntentUtils.share(this, mRepoOwner + "/" + mRepoName, url);
                return true;
            case R.id.browser:
                IntentUtils.launchBrowser(this, Uri.parse(url));
                return true;
            case R.id.search:
                String initialSearch = "repo:" + mRepoOwner + "/" + mRepoName + " ";
                startActivity(SearchActivity.makeIntent(this,
                        initialSearch, SearchActivity.SEARCH_TYPE_CODE));
                return true;
            case R.id.bookmark:
                String bookmarkUrl = getBookmarkUrl();
                if (BookmarksProvider.hasBookmarked(this, bookmarkUrl)) {
                    BookmarksProvider.removeBookmark(this, bookmarkUrl);
                } else {
                    BookmarksProvider.saveBookmark(this, mActionBar.getTitle().toString(),
                            BookmarksProvider.Columns.TYPE_REPO, bookmarkUrl, getCurrentRef(), true);
                }
                return true;
            case R.id.zip_download:
                String zipUrl = url + "/archive/" + getCurrentRef() + ".zip";
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
            Branch item = adapter.getItem(i);
            if (item.name().equals(mSelectedRef) || item.commit().sha().equals(mSelectedRef)) {
                current = i;
            }
            if (item.name().equals(mRepository.defaultBranch())) {
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
                        setSelectedRef(adapter.getItem(which).name());
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
        private final ArrayList<Branch> mItems;
        private final LayoutInflater mInflater;
        private final int mBranchDrawableResId;
        private final int mTagDrawableResId;
        private final int mFirstTagIndex;

        public BranchAndTagAdapter() {
            mItems = new ArrayList<>();
            mItems.addAll(mBranches);
            mItems.addAll(mTags);
            mFirstTagIndex = mBranches.size();
            mInflater = LayoutInflater.from(RepositoryActivity.this);
            mBranchDrawableResId = UiUtils.resolveDrawable(RepositoryActivity.this, R.attr.branchIcon);
            mTagDrawableResId = UiUtils.resolveDrawable(RepositoryActivity.this, R.attr.tagIcon);
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Branch getItem(int position) {
            return mItems.get(position);
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
            ImageView icon = convertView.findViewById(R.id.icon);
            TextView title = convertView.findViewById(R.id.title);

            icon.setImageResource(position >= mFirstTagIndex
                    ? mTagDrawableResId : mBranchDrawableResId);
            title.setText(mItems.get(position).name());

            return convertView;
        }
    }

    private class UpdateStarTask extends BackgroundTask<Void> {
        public UpdateStarTask() {
            super(RepositoryActivity.this);
        }

        @Override
        protected Void run() throws IOException {
            StarringService service = Gh4Application.get().getGitHubService(StarringService.class);
            Response<Void> response = mIsStarring
                    ? service.unstarRepository(mRepoOwner, mRepoName).blockingGet()
                    : service.starRepository(mRepoOwner, mRepoName).blockingGet();
            ApiHelpers.throwOnFailure(response);
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
            WatchingService service = Gh4Application.get().getGitHubService(WatchingService.class);
            final Response<?> response;
            if (mIsStarring) {
                response = service.deleteRepositorySubscription(mRepoOwner, mRepoName).blockingGet();
            } else {
                SubscriptionRequest request = SubscriptionRequest.builder()
                        .subscribed(true)
                        .ignored(false)
                        .build();
                response = service.setRepositorySubscription(mRepoOwner, mRepoName, request)
                        .blockingGet();
            }
            ApiHelpers.throwOnFailure(response);
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
