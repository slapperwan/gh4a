package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.CommitListFragment;

public class CommitHistoryActivity extends FragmentContainerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
                                    String ref, String path) {
        return new Intent(context, CommitHistoryActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("ref", ref)
                .putExtra("path", path);
    }

    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.history);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mFilePath;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
        mRef = extras.getString("ref");
        mFilePath = extras.getString("path");
    }

    @Override
    protected Fragment onCreateFragment() {
        return CommitListFragment.newInstance(mRepoOwner, mRepoName, mRef, mFilePath);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName, mRef);
    }
}