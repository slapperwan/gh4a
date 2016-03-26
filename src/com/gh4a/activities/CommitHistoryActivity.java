package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.utils.IntentUtils;

public class CommitHistoryActivity extends FragmentContainerActivity {
    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.history);
        actionBar.setSubtitle(mFilePath);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mRef = extras.getString(Constants.Object.REF);
        mFilePath = extras.getString(Constants.Object.PATH);
    }

    @Override
    protected Fragment onCreateFragment() {
        return CommitListFragment.newInstance(mRepoOwner, mRepoName, mRef, mFilePath);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, mRef);
    }
}