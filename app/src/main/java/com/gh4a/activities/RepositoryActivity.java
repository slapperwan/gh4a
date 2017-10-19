package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.gh4a.BaseFragmentPagerActivity;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.db.BookmarksProvider;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.fragment.ContentListContainerFragment;
import com.gh4a.fragment.RepositoryEventListFragment;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.utils.ApiHelpers;
import com.gh4a.utils.IntentUtils;
import com.gh4a.utils.RxUtils;
import com.gh4a.utils.UiUtils;
import com.meisolsson.githubsdk.model.Branch;
import com.meisolsson.githubsdk.model.Repository;
import com.meisolsson.githubsdk.model.request.activity.SubscriptionRequest;
import com.meisolsson.githubsdk.service.activity.StarringService;
import com.meisolsson.githubsdk.service.activity.WatchingService;
import com.meisolsson.githubsdk.service.repositories.RepositoryBranchService;
import com.meisolsson.githubsdk.service.repositories.RepositoryService;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;
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

    private static final int ID_LOADER_REPO = 0;
    private static final int ID_LOADER_WATCHING = 1;
    private static final int ID_LOADER_STARRING = 2;

    public static final int PAGE_REPO_OVERVIEW = 0;
    public static final int PAGE_FILES = 1;
    public static final int PAGE_COMMITS = 2;
    public static final int PAGE_ACTIVITY = 3;

    private static final int[] TITLES = new int[] {
        R.string.about, R.string.repo_files, R.string.commits, R.string.repo_activity
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

        loadRepository(false);
        loadStarringState(false);
        loadWatchingState(false);
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
        loadRepository(true);
        loadStarringState(true);
        loadWatchingState(true);
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
                toggleStarringState();
                return true;
            case R.id.star:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                toggleWatchingState();
                return true;
            case R.id.ref:
                loadOrShowRefSelection();
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
                .setSingleChoiceItems(adapter, current, (dialog, which) -> {
                    setSelectedRef(adapter.getItem(which).name());
                    dialog.dismiss();
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

    private void toggleStarringState() {
        StarringService service = Gh4Application.get().getGitHubService(StarringService.class);
        Single<Response<Void>> responseSingle = mIsStarring
                ? service.unstarRepository(mRepoOwner, mRepoName)
                : service.starRepository(mRepoOwner, mRepoName);
        responseSingle.map(ApiHelpers::mapToBooleanOrThrowOnFailure)
                .compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsStarring != null) {
                        mIsStarring = !mIsStarring;
                        if (mRepositoryFragment != null) {
                            mRepositoryFragment.updateStargazerCount(mIsStarring);
                        }
                        supportInvalidateOptionsMenu();
                    }
                }, error -> supportInvalidateOptionsMenu());

    }

    private void toggleWatchingState() {
        WatchingService service = Gh4Application.get().getGitHubService(WatchingService.class);
        final Single<?> responseSingle;

        if (mIsWatching) {
            responseSingle = service.deleteRepositorySubscription(mRepoOwner, mRepoName)
                    .map(ApiHelpers::throwOnFailure);
        } else {
            SubscriptionRequest request = SubscriptionRequest.builder()
                    .subscribed(true)
                    .build();
            responseSingle = service.setRepositorySubscription(mRepoOwner, mRepoName, request)
                    .map(ApiHelpers::throwOnFailure);
        }

        responseSingle.compose(RxUtils::doInBackground)
                .subscribe(result -> {
                    if (mIsWatching == null) {
                        mIsWatching = !mIsWatching;
                    }
                    supportInvalidateOptionsMenu();
                }, error -> supportInvalidateOptionsMenu());
    }

    private void loadRepository(boolean force) {
        RepositoryService service = Gh4Application.get().getGitHubService(RepositoryService.class);
        service.getRepository(mRepoOwner, mRepoName)
                .map(ApiHelpers::throwOnFailure)
                .compose(makeLoaderSingle(ID_LOADER_REPO, force))
                .subscribe(result -> {
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
                }, error -> {});
    }

    private void loadStarringState(boolean force) {
        Gh4Application app = Gh4Application.get();
        if (!app.isAuthorized()) {
            return;
        }
        StarringService service = app.getGitHubService(StarringService.class);
        service.checkIfRepositoryIsStarred(mRepoOwner, mRepoName)
                .map(ApiHelpers::throwOnFailure)
                // success response means 'starred'
                .map(result -> true)
                // 404 means 'not starred'
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, false))
                .compose(makeLoaderSingle(ID_LOADER_STARRING, force))
                .subscribe(result -> {
                    mIsStarring = result;
                    supportInvalidateOptionsMenu();
                }, error -> {});
    }

    private void loadWatchingState(boolean force) {
        Gh4Application app = Gh4Application.get();
        if (!app.isAuthorized()) {
            return;
        }
        WatchingService service = app.getGitHubService(WatchingService.class);
        service.getRepositorySubscription(mRepoOwner, mRepoName)
                .map(ApiHelpers::throwOnFailure)
                .map(subscription -> subscription.subscribed())
                // 404 means 'not subscribed'
                .compose(RxUtils.mapFailureToValue(HttpURLConnection.HTTP_NOT_FOUND, false))
                .compose(makeLoaderSingle(ID_LOADER_WATCHING, force))
                .subscribe(result -> {
                    mIsWatching = result;
                    supportInvalidateOptionsMenu();
                }, error -> {});
    }

    private void loadOrShowRefSelection() {
        if (mBranches != null) {
            showRefSelectionDialog();
        } else {
            Gh4Application app = Gh4Application.get();
            final RepositoryBranchService branchService =
                    app.getGitHubService(RepositoryBranchService.class);
            final RepositoryService repoService = app.getGitHubService(RepositoryService.class);

            Single<List<Branch>> branchSingle = ApiHelpers.PageIterator
                    .toSingle(page -> branchService.getBranches(mRepoOwner, mRepoName, page));
            Single<List<Branch>> tagSingle = ApiHelpers.PageIterator
                    .toSingle(page -> repoService.getTags(mRepoOwner, mRepoName, page));

            registerTemporarySubscription(Single.zip(branchSingle, tagSingle, (branches, tags) -> Pair.create(branches, tags))
                    .compose(RxUtils::doInBackground)
                    .compose(RxUtils.wrapWithProgressDialog(this, R.string.loading_msg))
                    .subscribe(result -> {
                        mBranches = result.first;
                        mTags = result.second;
                        showRefSelectionDialog();
                    }, error -> {}));
        }
    }
}
