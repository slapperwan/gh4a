package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;

import com.gh4a.Constants;
import com.gh4a.R;
import com.gh4a.fragment.CommitListFragment;

public class CommitHistoryActivity extends BaseFragmentActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.generic_list);

        Bundle extras = getIntent().getExtras();
        String filePath = extras.getString(Constants.Object.PATH);

        if (savedInstanceState == null) {
            CommitListFragment fragment = CommitListFragment.newInstance(
                    extras.getString(Constants.Repository.OWNER),
                    extras.getString(Constants.Repository.NAME),
                    extras.getString(Constants.Object.REF),
                    filePath);

            FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().add(android.R.id.content, fragment).commit();
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.history);
        actionBar.setSubtitle(filePath);
    }
}