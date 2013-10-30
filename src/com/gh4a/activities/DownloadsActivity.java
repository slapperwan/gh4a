package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.R;
import com.gh4a.fragment.DownloadBranchesFragment;
import com.gh4a.fragment.DownloadTagsFragment;
import com.gh4a.fragment.DownloadsFragment;

public class DownloadsActivity extends BaseSherlockFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        
        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        
        if (!isOnline()) {
            setErrorView();
            return;
        }
        
        setContentView(R.layout.view_pager);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.downloads);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);

        setupPager(new ThisPageAdapter(getSupportFragmentManager()), new int[] {
            R.string.packages, R.string.repo_branches, R.string.repo_tags
        });
    }
    
    private class ThisPageAdapter extends FragmentStatePagerAdapter {
        public ThisPageAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {
            if (position == 1) {
                return DownloadBranchesFragment.newInstance(mRepoOwner, mRepoName);
            } else if (position == 2) {
                return DownloadTagsFragment.newInstance(mRepoOwner, mRepoName);
            } else {
                return DownloadsFragment.newInstance(mRepoOwner, mRepoName);
            }
        }
    }
    
    @Override
    protected void navigateUp() {
        finish();
    }
}