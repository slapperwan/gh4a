package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.DownloadsFragment;

public class DownloadsActivity extends FragmentContainerActivity {
    public static Intent makeIntent(Context context, String repoOwner, String repoName) {
        return new Intent(context, DownloadsActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName);
    }

    private String mRepoOwner;
    private String mRepoName;

    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.downloads);
    }

    @Nullable
    @Override
    protected String getActionBarSubtitle() {
        return mRepoOwner + "/" + mRepoName;
    }

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("owner");
        mRepoName = extras.getString("repo");
    }

    @Override
    protected Fragment onCreateFragment() {
        return DownloadsFragment.newInstance(mRepoOwner, mRepoName);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName);
    }
}
