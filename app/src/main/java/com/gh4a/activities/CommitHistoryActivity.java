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

    private static final String EXTRA_OWNER = "owner";
    private static final String EXTRA_REPO = "repo";
    private static final String EXTRA_REF = "ref";
    private static final String EXTRA_PATH = "path";
    private static final String EXTRA_BASE_SELECTABLE = "base_selectable";
    private static final String EXTRA_COMMIT = "commit";

    public static Intent makeIntent(Context context, String repoOwner, String repoName,
                                    String ref, String path, boolean supportBaseSelection) {
        return new Intent(context, CommitHistoryActivity.class)
                .putExtra(EXTRA_OWNER, repoOwner)
                .putExtra(EXTRA_REPO, repoName)
                .putExtra(EXTRA_REF, ref)
                .putExtra(EXTRA_PATH, path)
                .putExtra(EXTRA_BASE_SELECTABLE, supportBaseSelection);
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
        mRepoOwner = extras.getString(EXTRA_OWNER);
        mRepoName = extras.getString(EXTRA_REPO);
        mRef = extras.getString(EXTRA_REF);
        mFilePath = extras.getString(EXTRA_PATH);
        mSupportBaseSelection = extras.getBoolean(EXTRA_BASE_SELECTABLE);
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
        result.putExtra(EXTRA_COMMIT, commit);
        setResult(RESULT_OK, result);
        finish();
    }
}