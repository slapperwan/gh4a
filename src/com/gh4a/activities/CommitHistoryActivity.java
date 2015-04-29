package com.gh4a.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;

import com.gh4a.BaseActivity;
import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.CommitListFragment;
import com.gh4a.utils.IntentUtils;

public class CommitHistoryActivity extends BaseActivity {
    private String mRepoOwner;
    private String mRepoName;
    private String mRef;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasErrorView()) {
            return;
        }

        setContentView(R.layout.generic_list);

        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString(Constants.Object.PATH);
        mRepoOwner = extras.getString(Constants.Repository.OWNER);
        mRepoName = extras.getString(Constants.Repository.NAME);
        mRef = extras.getString(Constants.Object.REF);

        if (savedInstanceState == null) {
            CommitListFragment fragment = CommitListFragment.newInstance(mRepoOwner,
                    mRepoName, mRef, filePath);

            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().add(R.id.content_container, fragment).commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.history);
        actionBar.setSubtitle(filePath);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected Intent navigateUp() {
        return IntentUtils.getRepoActivityIntent(this, mRepoOwner, mRepoName, mRef);
    }
}