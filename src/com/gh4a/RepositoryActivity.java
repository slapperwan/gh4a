package com.gh4a;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.github.core.TreeEntry;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.gh4a.fragment.ContentListFragment;
import com.gh4a.fragment.ContentListFragment.OnTreeSelectedListener;
import com.gh4a.fragment.RepositoryFragment;
import com.gh4a.utils.StringUtils;

public class RepositoryActivity extends BaseSherlockFragmentActivity
    implements OnTreeSelectedListener {

    private static final int NUM_ITEMS = 3;
    private String mRepoOwner;
    private String mRepoName;
    private RepositoryAdapter mAdapter;
    private ViewPager mPager;
    private ActionBar mActionBar;
    private List<ContentListFragment> fileStacks;//to keep track folders for use in on back pressed
    private List<List<TreeEntry>> treeEntriesList;
    private boolean backPressed;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager);
        
        fileStacks = new ArrayList<ContentListFragment>();
        treeEntriesList = new ArrayList<List<TreeEntry>>();
        
        Bundle data = getIntent().getExtras().getBundle(Constants.DATA_BUNDLE);
        if (data != null) {
            mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
            mRepoName = data.getString(Constants.Repository.REPO_NAME);
        }
        else {
            Bundle bundle = getIntent().getExtras();
            mRepoOwner = bundle.getString(Constants.Repository.REPO_OWNER);
            mRepoName = bundle.getString(Constants.Repository.REPO_NAME);
        }
        
        mActionBar = getSupportActionBar();
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
        
        mActionBar.setTitle(mRepoName);
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setHomeButtonEnabled(true);
        
        Tab tab = mActionBar
                .newTab()
                .setText(R.string.about)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 0 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(R.string.repo_files)
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 1 + "", mPager));
        mActionBar.addTab(tab);
        
        tab = mActionBar
                .newTab()
                .setText(getResources().getQuantityString(R.plurals.commit, 2))
                .setTabListener(
                        new TabListener<SherlockFragmentActivity>(this, 2 + "", mPager));
        mActionBar.addTab(tab);
        
    }
    
    public class RepositoryAdapter extends FragmentStatePagerAdapter {

        public ContentListFragment mFragmentFiles;
        public String mSha;
        
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
                return RepositoryFragment.newInstance(mRepoOwner, mRepoName);
            }
            
            else if (position == 1) {
                if (mFragmentFiles == null) {
                    mFragmentFiles = ContentListFragment.newInstance(mRepoOwner, mRepoName, null);
                    fileStacks.add(mFragmentFiles);
                }
                
                else if (backPressed) {
                    fileStacks.remove(mFragmentFiles);
                    getSupportFragmentManager().beginTransaction().remove(mFragmentFiles).commit();
                    mFragmentFiles = fileStacks.get(fileStacks.size() - 1);
                    mFragmentFiles.setTreeEntryList(treeEntriesList.get(fileStacks.size() - 1));
                }
                
                else {
                    getSupportFragmentManager().beginTransaction().remove(mAdapter.mFragmentFiles).commit();
                    mFragmentFiles = ContentListFragment.newInstance(mRepoOwner, mRepoName, mSha);
                    fileStacks.add(mFragmentFiles);
                }
                return mFragmentFiles;
            }
            
            else if (position == 2) {
                return RepositoryFragment.newInstance(mRepoOwner, mRepoName);
            }

            else {
                return RepositoryFragment.newInstance(mRepoOwner, mRepoName);
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
            if (object instanceof ContentListFragment && !StringUtils.isBlank(mSha)) {
                return POSITION_NONE;
            }
            return POSITION_UNCHANGED;
        }
    }

    @Override
    public void onTreeSelected(int position, AdapterView<?> adapterView,
            TreeEntry treeEntry, List<TreeEntry> entries) {
        backPressed = false;
        mAdapter.mSha = treeEntry.getSha();
        treeEntriesList.add(entries);
        mAdapter.notifyDataSetChanged();
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
        backPressed = true;
        if (fileStacks.size() > 1) {
            mAdapter.notifyDataSetChanged();
            return;
        }
        else {
            super.onBackPressed();
        }
    }
}
