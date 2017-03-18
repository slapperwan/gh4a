package com.gh4a.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;

import com.gh4a.R;
import com.gh4a.fragment.BookmarkListFragment;

public class BookmarkListActivity extends FragmentContainerActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.bookmarks);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected Fragment onCreateFragment() {
        return BookmarkListFragment.newInstance();
    }

    @Override
    protected boolean canSwipeToRefresh() {
        // content can't change while we're in foreground
        return false;
    }
}