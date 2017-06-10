package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.gh4a.fragment.ReviewCommentsFragment;
import com.gh4a.loader.TimelineItem;

import java.util.ArrayList;

public class ReviewCommentsActivity extends FragmentContainerActivity {

    public static Intent makeIntent(Context context, String repoOwner, boolean isPullRequest,
            ArrayList<TimelineItem.Diff> chunks) {
        return new Intent(context, ReviewCommentsActivity.class)
                .putExtra("repo_owner", repoOwner)
                .putExtra("is_pr", isPullRequest)
                .putExtra("chunks", chunks);
    }

    private String mRepoOwner;
    private boolean mIsPullRequest;
    private ArrayList<TimelineItem.Diff> mChunks;

    @Override
    protected void onInitExtras(Bundle extras) {
        super.onInitExtras(extras);
        mRepoOwner = extras.getString("repo_owner");
        mIsPullRequest = extras.getBoolean("is_pr");
        mChunks = (ArrayList<TimelineItem.Diff>) extras.getSerializable("chunks");
    }

    @Override
    protected Fragment onCreateFragment() {
        return ReviewCommentsFragment.newInstance(mRepoOwner, mIsPullRequest, mChunks);
    }

    @Override
    protected Intent navigateUp() {
        // TODO
        return super.navigateUp();
    }
}
