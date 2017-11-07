package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.gh4a.R;
import com.gh4a.fragment.CommitListFragment;
import com.meisolsson.githubsdk.model.Commit;

public class CommitHistoryActivity extends FragmentContainerActivity implements
        CommitListFragment.ContextSelectionCallback {
    public static Intent makeIntent(Context context, String repoOwner, String repoName,
                                    String ref, String path, boolean supportBaseSelection) {
        return new Intent(context, CommitHistoryActivity.class)
                .putExtra("owner", repoOwner)
                .putExtra("repo", repoName)
                .putExtra("ref", ref)
                .putExtra("path", path)
                .putExtra("base_selectable", supportBaseSelection);
    }

    private String mRepoOwner;
    private String mRepoName;
    private String mRef;
    private String mFilePath;
    private boolean mSupportBaseSelection;

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
        mSupportBaseSelection = extras.getBoolean("base_selectable");
    }

    @Override
    protected Fragment onCreateFragment() {
        return CommitListFragment.newInstance(mRepoOwner, mRepoName, mRef, mFilePath);
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName, mRef);
    }

    @Override
    public boolean baseSelectionAllowed() {
        return mSupportBaseSelection;
    }

    @Override
    public void onCommitSelectedAsBase(Commit commit) {
        Intent result = new Intent();
        result.putExtra("commit", commit);
        setResult(RESULT_OK, result);
        finish();
    }
}