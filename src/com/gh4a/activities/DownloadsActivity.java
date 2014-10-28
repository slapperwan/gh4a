package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;

import com.gh4a.Constants;
import com.gh4a.LoadingFragmentActivity;
import com.gh4a.R;
import com.gh4a.fragment.DownloadsFragment;
import com.gh4a.utils.IntentUtils;

public class DownloadsActivity extends LoadingFragmentActivity {
    private String mRepoOwner;
    private String mRepoName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        Bundle data = getIntent().getExtras();
        mRepoOwner = data.getString(Constants.Repository.OWNER);
        mRepoName = data.getString(Constants.Repository.NAME);

        setContentView(R.layout.frame_layout);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.details, DownloadsFragment.newInstance(mRepoOwner, mRepoName))
                    .commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.downloads);
        actionBar.setSubtitle(mRepoOwner + "/" + mRepoName);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, null);
    }
}
