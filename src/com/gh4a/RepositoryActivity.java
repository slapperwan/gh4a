package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.Content;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryTag;

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
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.fragment.ContentListFragment;
import com.gh4a.fragment.ContentListFragment.OnTreeSelectedListener;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.loader.BranchListLoader;
import com.gh4a.loader.IsWatchingLoader;
import com.gh4a.loader.RepositoryLoader;
import com.gh4a.loader.TagListLoader;
import com.gh4a.loader.WatchLoader;
import com.gh4a.utils.StringUtils;

public class RepositoryActivity extends BaseSherlockFragmentActivity
    implements OnTreeSelectedListener, LoaderManager.LoaderCallbacks {

    private static final int NUM_ITEMS = 3;
    private String mRepoOwner;
    private String mRepoName;
    private RepositoryAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private List<ContentListFragment> fileStacks;//to keep track folders for use in on back pressed
    private List<List<Content>> mContentList;
    private boolean backPressed;
    private Repository mRepository;
    private List<RepositoryBranch> mBranches;
    private List<RepositoryTag> mTags;
    private String mSelectedRef;
    private String mSelectBranchTag;
    private ProgressDialog mProgressDialog;
    private boolean isFinishLoadingWatching;
    private boolean isWatching;
    private RepositoryFragment mRepositoryFragment;
    private ContentListFragment mContentListFragment;
    private CommitListFragment mCommitListFragment;
    private String mPath;
    private int mCurrentTab;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        fileStacks = new ArrayList<ContentListFragment>();
        mContentList = new ArrayList<List<Content>>();
        
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
        
        setContentView(R.layout.view_pager_repo);
        
        mActionBar = getSupportActionBar();
        mActionBar.setTitle(mRepoOwner + "/" + mRepoName);
        mActionBar.setDisplayHomeAsUpEnabled(true);
        
        showLoading();
        getSupportLoaderManager().initLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
        
        getSupportLoaderManager().initLoader(1, null, this);
        getSupportLoaderManager().initLoader(2, null, this);
        
        getSupportLoaderManager().initLoader(3, null, this);
        getSupportLoaderManager().getLoader(3).forceLoad();
        
        getSupportLoaderManager().initLoader(4, null, this);
    }
    
    private void fillTabs() {
        mActionBar.removeAllTabs();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setSubtitle(StringUtils.isBlank(mSelectBranchTag) ?
                mRepository.getMasterBranch() : mSelectBranchTag);
        
        mAdapter = new RepositoryAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        
        mPager.setOnPageChangeListener(new OnPageChangeListener() {

            @Override
            public void onPageScrollStateChanged(int arg0) {}
            
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {}

            @Override
            public void onPageSelected(int arg0) {
                mActionBar.getTabAt(arg0).select();
            }
        });
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.about)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 0);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.repo_files)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 1);
        
        tab = mActionBar
                .newTab()
                .setText(getResources().getQuantityString(R.plurals.commit, 2))
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab, mCurrentTab == 2);
    }
    
    public class RepositoryAdapter extends FragmentStatePagerAdapter {

        public Content mContent;
        
        public RepositoryAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 0) {
                mRepositoryFragment = RepositoryFragment.newInstance(mRepository);
                return mRepositoryFragment;
            }
            
            else if (position == 1) {
                if (mContentListFragment == null) {
                    mContentListFragment = ContentListFragment.newInstance(mRepository, null, mSelectedRef);
                    fileStacks.add(mContentListFragment);
                }
                
                else if (backPressed) {
                    fileStacks.remove(mContentListFragment);
                    getSupportFragmentManager().beginTransaction().remove(mContentListFragment).commit();
                    mContentListFragment = fileStacks.get(fileStacks.size() - 1);
                    mContentListFragment.setTreeEntryList(mContentList.get(fileStacks.size() - 1));
                }
                
                else {
                    getSupportFragmentManager().beginTransaction().remove(mContentListFragment).commit();
                    mContentListFragment = ContentListFragment.newInstance(mRepository, 
                            mPath, mSelectedRef);
                    fileStacks.add(mContentListFragment);
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
            if (object instanceof ContentListFragment && !StringUtils.isBlank(mContent.getSha())) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    @Override
    public void onTreeSelected(int position, AdapterView<?> adapterView,
            Content content, List<Content> contents, String ref) {
        if ("dir".equals(content.getType())) {
            backPressed = false;
            mAdapter.mContent = content;
            mSelectedRef = ref;
            mPath = content.getPath();
            mContentList.add(contents);
            mAdapter.notifyDataSetChanged();
        }
        else {
            Intent intent = new Intent().setClass(this, FileViewerActivity.class);
            intent.putExtra(Constants.Repository.REPO_OWNER, mRepoOwner);
            intent.putExtra(Constants.Repository.REPO_NAME, mRepoName);
            intent.putExtra(Constants.Object.PATH, content.getPath());
            intent.putExtra(Constants.Object.REF, ref);
            intent.putExtra(Constants.Object.NAME, content.getName());
            intent.putExtra(Constants.Object.OBJECT_SHA, content.getSha());
            startActivity(intent);
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            onBackPressed();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (mPager != null) {
            if (mPager.getCurrentItem() == 1) {
                backPressed = true;
                if (mPath != null && mPath.lastIndexOf("/") != -1) {
                    mPath = mPath.substring(0, mPath.lastIndexOf("/"));
                }
                else {
                    mPath = null;
                }
                
                if (fileStacks.size() > 1) {
                    mAdapter.notifyDataSetChanged();
                    return;
                }
                else {
                    super.onBackPressed();
                }
            }
            else {
                super.onBackPressed();
            }
        }
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.repo_menu, menu);
        
        MenuItem watchAction = menu.getItem(1);
        if (!isFinishLoadingWatching) {
            watchAction.setActionView(R.layout.ab_loading);
            watchAction.expandActionView();
        }
        else {
            if (isWatching) {
                watchAction.setTitle(R.string.repo_unwatch_action);
            }
            else {
                watchAction.setTitle(R.string.repo_watch_action);
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean setMenuOptionItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getApplicationContext().openUserInfoActivity(this, mRepoOwner, null, Intent.FLAG_ACTIVITY_CLEAR_TOP);
                return true;
            case R.id.watch:
                item.setActionView(R.layout.ab_loading);
                item.expandActionView();
                getSupportLoaderManager().restartLoader(4, null, this);
                getSupportLoaderManager().getLoader(4).forceLoad();
                return true;
            case R.id.branches:
                if (mBranches == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(1).forceLoad();
                }
                else {
                    showBranchesDialog();
                }
                return true;
            case R.id.tags:
                if (mTags == null) {
                    mProgressDialog = showProgressDialog(getString(R.string.loading_msg), true);
                    getSupportLoaderManager().getLoader(2).forceLoad();
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
            default:
                return true;
        }
    }
    
    @Override
    public Loader onCreateLoader(int id, Bundle bundle) {
        if (id == 0) {
            return new RepositoryLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 1) {
            return new BranchListLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 2) {
            return new TagListLoader(this, mRepoOwner, mRepoName);
        }
        else if (id == 3) {
            return new IsWatchingLoader(this, mRepoOwner, mRepoName);
        }
        else {
            return new WatchLoader(this, mRepoOwner, mRepoName, isWatching);
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object object) {
        if (loader.getId() == 0) {
            hideLoading();
            if (object != null) {
                this.mRepository = (Repository) object;
                fillTabs();
            }
            invalidateOptionsMenu();
        }
        else if (loader.getId() == 1) {
            stopProgressDialog(mProgressDialog);
            if (object != null) {
                this.mBranches = (List<RepositoryBranch>) object;
                showBranchesDialog();
            }
        }
        else if (loader.getId() == 2) {
            stopProgressDialog(mProgressDialog);
            if (object != null) {
                this.mTags = (List<RepositoryTag>) object;
                showTagsDialog();
            }
        }
        else if (loader.getId() == 3) {
            if (object != null) {
                isWatching = (Boolean) object;
                isFinishLoadingWatching = true;
            }
            invalidateOptionsMenu();
        }
        else {
            if (object != null) {
                isWatching = (Boolean) object;
                isFinishLoadingWatching = true;
                if (mRepositoryFragment != null) {
                    mRepositoryFragment.updateWatcherCount(isWatching);
                }
            }
            invalidateOptionsMenu();
        }
    }

    @Override
    public void onLoaderReset(Loader arg0) {
        // TODO Auto-generated method stub
        
    }
    
    private void showBranchesDialog() {
        String[] branchList = new String[mBranches.size()];
        for (int i = 0; i < mBranches.size(); i++) {
            branchList[i] = mBranches.get(i).getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.repo_branches);
        builder.setSingleChoiceItems(branchList, -1, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedRef = mBranches.get(which).getCommit().getSha();
                mSelectBranchTag = mBranches.get(which).getName();
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                refreshFragment();
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    private void showTagsDialog() {
        String[] tagList = new String[mTags.size()];
        for (int i = 0; i < mTags.size(); i++) {
            tagList[i] = mTags.get(i).getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, android.R.style.Theme));
        builder.setCancelable(true);
        builder.setTitle(R.string.repo_tags);
        builder.setSingleChoiceItems(tagList, -1, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mSelectedRef = mTags.get(which).getCommit().getSha();
                mSelectBranchTag = mTags.get(which).getName();
            }
        });
        
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                refreshFragment();
                dialog.dismiss();
            }
        })
        .setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        })
       .create();
        
        builder.show();
    }
    
    private void refreshFragment() {
        mCurrentTab = mPager.getCurrentItem();
        mRepositoryFragment = null;
        mContentListFragment = null;
        mCommitListFragment = null;
        fileStacks.clear();
        mPath = null;
        showLoading();
        getSupportLoaderManager().restartLoader(0, null, this);
        getSupportLoaderManager().getLoader(0).forceLoad();
    }
}
