package com.gh4a.activities;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.BookmarkListFragment;

public class BookmarkListActivity extends FragmentContainerActivity {
    @Nullable
    @Override
    protected String getActionBarTitle() {
        return getString(R.string.bookmarks);
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