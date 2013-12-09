package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.ActionBar;
import com.gh4a.Constants;
import com.gh4a.Gh4Application;
import com.gh4a.LoadingFragmentPagerActivity;
import com.gh4a.R;
import com.gh4a.fragment.DownloadBranchesFragment;
import com.gh4a.fragment.DownloadTagsFragment;
import com.gh4a.fragment.DownloadsFragment;

public class DownloadsActivity extends LoadingFragmentPagerActivity {
    private String mRepoOwner;
    private String mRepoName;

    private static final int[] TITLES = new int[] {
        R.string.packages, R.string.repo_branches, R.string.repo_tags
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Gh4Application.THEME);
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.REPO_OWNER);
        mRepoName = data.getString(Constants.Repository.REPO_NAME);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.downloads);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int[] getTabTitleResIds() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        switch (position) {
            case 0: return DownloadsFragment.newInstance(mRepoOwner, mRepoName);
            case 1: return DownloadBranchesFragment.newInstance(mRepoOwner, mRepoName);
            case 2: return DownloadTagsFragment.newInstance(mRepoOwner, mRepoName);
        }
        return null;
    }
    
    @Override
    protected void navigateUp() {
        finish();
    }
}