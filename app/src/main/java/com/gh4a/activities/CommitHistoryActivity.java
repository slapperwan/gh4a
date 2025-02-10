package com.gh4a.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
    private boolean mFollowRenames;

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
        CommitListFragment f = CommitListFragment.newInstance(mRepoOwner, mRepoName, mRef, mFilePath);
        f.setFollowFileRenames(mFollowRenames);
        return f;
    }

    @Override
    protected Intent navigateUp() {
        return RepositoryActivity.makeIntent(this, mRepoOwner, mRepoName, mRef);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.commit_history_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem followItem = menu.findItem(R.id.follow_renames);
        if (followItem != null) {
            followItem.setChecked(mFollowRenames);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.follow_renames) {
            mFollowRenames = !mFollowRenames;
            item.setChecked(mFollowRenames);
            CommitListFragment f = (CommitListFragment) getFragment();
            if (f != null) {
                f.setFollowFileRenames(mFollowRenames);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
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